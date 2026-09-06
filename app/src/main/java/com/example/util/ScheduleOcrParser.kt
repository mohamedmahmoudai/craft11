package com.example.util

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ImportedScheduleDraft(
    val id: String = UUID.randomUUID().toString(),
    var title: String,
    var subtitle: String = "",
    var dayOfWeek: Int = Calendar.SATURDAY, // Calendar day constant
    var dayName: String = "السبت",
    var timeText: String = "08:00 ص",
    var startTime: String = "08:00",
    var endTime: String = "09:00",
    var durationMinutes: Int = 60,
    var isFixed: Boolean = true,
    var isWeeklyRecurring: Boolean = true,
    var isSelected: Boolean = true
)

object ScheduleOcrParser {

    private enum class DayInfo(val calDay: Int, val arabicName: String, val keywords: List<String>) {
        SATURDAY(Calendar.SATURDAY, "السبت", listOf("saturday", "sat", "السبت")),
        SUNDAY(Calendar.SUNDAY, "الأحد", listOf("sunday", "sun", "الأحد", "الاحد")),
        MONDAY(Calendar.MONDAY, "الإثنين", listOf("monday", "mon", "الإثنين", "الاثنين")),
        TUESDAY(Calendar.TUESDAY, "الثلاثاء", listOf("tuesday", "tue", "الثلاثاء")),
        WEDNESDAY(Calendar.WEDNESDAY, "الأربعاء", listOf("wednesday", "wed", "الأربعاء", "الاربعاء")),
        THURSDAY(Calendar.THURSDAY, "الخميس", listOf("thursday", "thu", "الخميس")),
        FRIDAY(Calendar.FRIDAY, "الجمعة", listOf("friday", "fri", "الجمعة"))
    }

    private data class OcrLine(
        val text: String,
        val rect: Rect
    )

    private data class DayRow(
        val dayInfo: DayInfo,
        val yCenter: Int,
        val rect: Rect
    )

    private data class HourColumn(
        val startHour: Int,
        val startMin: Int,
        val endHour: Int,
        val endMin: Int,
        val durationMinutes: Int,
        val formattedTime: String,
        val startTimeStr: String,
        val endTimeStr: String,
        val xCenter: Int,
        val rect: Rect
    )

    /**
     * Process image URI with ML Kit Text Recognition asynchronously
     */
    suspend fun recognizeTextFromUri(context: Context, uri: Uri): Result<Text> =
        suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            continuation.resume(Result.success(visionText))
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(exception))
                        }
                    }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }

    /**
     * Parses ML Kit Vision Text into structured schedule draft items using matrix grid algorithm
     */
    fun parseScheduleItems(visionText: Text): List<ImportedScheduleDraft> {
        val allLines = mutableListOf<OcrLine>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val t = line.text.trim()
                val r = line.boundingBox
                if (t.isNotEmpty() && r != null) {
                    allLines.add(OcrLine(t, r))
                }
            }
        }

        if (allLines.isEmpty()) return emptyList()

        // 1. Identify Row Headers (Days of week)
        val dayRows = mutableListOf<DayRow>()
        val dayLinesIndices = mutableSetOf<Int>()

        for ((idx, line) in allLines.withIndex()) {
            val matchedDay = matchDay(line.text)
            if (matchedDay != null) {
                // Ensure we don't have duplicate row headers too close together
                val existing = dayRows.firstOrNull { it.dayInfo == matchedDay }
                if (existing == null) {
                    dayRows.add(DayRow(matchedDay, line.rect.centerY(), line.rect))
                    dayLinesIndices.add(idx)
                }
            }
        }
        dayRows.sortBy { it.yCenter }

        // 2. Identify Column Headers (Hours)
        val hourColumns = mutableListOf<HourColumn>()
        val hourLinesIndices = mutableSetOf<Int>()

        for ((idx, line) in allLines.withIndex()) {
            if (idx in dayLinesIndices) continue
            val col = parseHourHeader(line.text, line.rect)
            if (col != null) {
                // Avoid near-duplicate column headers
                val existing = hourColumns.firstOrNull { Math.abs(it.xCenter - col.xCenter) < 40 }
                if (existing == null) {
                    hourColumns.add(col)
                    hourLinesIndices.add(idx)
                }
            }
        }
        hourColumns.sortBy { it.xCenter }

        // Adjust 12-hour/24-hour progression for columns (e.g. 8-9, 9-10, 10-11, 11-12, 12-1, 1-2, 2-4)
        var passedNoon = false
        for (i in hourColumns.indices) {
            val col = hourColumns[i]
            var sH = col.startHour
            var eH = col.endHour

            if (sH == 12) passedNoon = true
            if (passedNoon || sH in 1..6) {
                if (sH in 1..6) sH += 12
                if (eH in 1..7) eH += 12
            }
            val dur = ((eH * 60 + col.endMin) - (sH * 60 + col.startMin)).coerceIn(30, 240)
            val sStr = String.format(Locale.US, "%02d:%02d", sH, col.startMin)
            val eStr = String.format(Locale.US, "%02d:%02d", eH, col.endMin)
            val formatted = formatTimeDisplay(sH, col.startMin, eH, col.endMin)

            hourColumns[i] = col.copy(
                startHour = sH,
                endHour = eH,
                durationMinutes = dur,
                startTimeStr = sStr,
                endTimeStr = eStr,
                formattedTime = formatted
            )
        }

        // 3. Matrix Matching: if both days and hour columns detected
        if (dayRows.isNotEmpty() && hourColumns.isNotEmpty()) {
            return parseMatrixGrid(allLines, dayRows, hourColumns, dayLinesIndices, hourLinesIndices)
        }

        // 4. Fallback: Day-grouped or linear parsing with strict noise filtering
        return parseFallbackLinear(allLines, dayRows, dayLinesIndices)
    }

    private fun parseMatrixGrid(
        allLines: List<OcrLine>,
        dayRows: List<DayRow>,
        hourColumns: List<HourColumn>,
        dayIndices: Set<Int>,
        hourIndices: Set<Int>
    ): List<ImportedScheduleDraft> {
        val result = mutableListOf<ImportedScheduleDraft>()

        // Map of (DayIndex, ColumnIndex) -> List of OcrLine
        val cellMap = mutableMapOf<Pair<Int, Int>, MutableList<OcrLine>>()

        for ((idx, line) in allLines.withIndex()) {
            if (idx in dayIndices || idx in hourIndices) continue
            if (isHeaderNoise(line.text)) continue

            val lineY = line.rect.centerY()
            val lineX = line.rect.centerX()

            // Find closest day row vertically
            var bestDayIdx = -1
            var minDy = Int.MAX_VALUE
            for ((dIdx, dRow) in dayRows.withIndex()) {
                val dy = Math.abs(dRow.yCenter - lineY)
                if (dy < minDy) {
                    minDy = dy
                    bestDayIdx = dIdx
                }
            }

            // Find closest hour column horizontally
            var bestColIdx = -1
            var minDx = Int.MAX_VALUE
            for ((cIdx, hCol) in hourColumns.withIndex()) {
                val dx = Math.abs(hCol.xCenter - lineX)
                if (dx < minDx) {
                    minDx = dx
                    bestColIdx = cIdx
                }
            }

            if (bestDayIdx != -1 && bestColIdx != -1) {
                // Ensure not wildly out of bounds
                cellMap.getOrPut(Pair(bestDayIdx, bestColIdx)) { mutableListOf() }.add(line)
            }
        }

        // Convert grouped cells into drafts
        for ((cellKey, lines) in cellMap) {
            val (dayIdx, colIdx) = cellKey
            val dayRow = dayRows[dayIdx]
            val hourCol = hourColumns[colIdx]

            // Sort lines in cell from top to bottom
            lines.sortBy { it.rect.top }

            val cleanLines = lines.map { it.text.trim() }
                .filter { it.length >= 2 && !isHeaderNoise(it) && !isTrivialPlaceholder(it) }

            if (cleanLines.isEmpty()) continue

            // First line is the Subject Name
            val subjectTitle = cleanLines.first()
                .trim(' ', '-', ':', '•', '|', '[', ']', '(', ')')

            // Subsequent lines are Location / Notes (e.g. "Class II G 1,2,3,4")
            val notesLocation = if (cleanLines.size > 1) {
                cleanLines.drop(1).joinToString(" | ")
            } else ""

            if (subjectTitle.isNotBlank() && subjectTitle.length >= 3) {
                result.add(
                    ImportedScheduleDraft(
                        title = subjectTitle,
                        subtitle = notesLocation,
                        dayOfWeek = dayRow.dayInfo.calDay,
                        dayName = dayRow.dayInfo.arabicName,
                        timeText = hourCol.formattedTime,
                        startTime = hourCol.startTimeStr,
                        endTime = hourCol.endTimeStr,
                        durationMinutes = hourCol.durationMinutes,
                        isFixed = true,
                        isWeeklyRecurring = true,
                        isSelected = true
                    )
                )
            }
        }

        // Return sorted by Day of week, then start time
        result.sortWith(compareBy({ it.dayOfWeek }, { it.startTime }))
        return result
    }

    private fun parseFallbackLinear(
        allLines: List<OcrLine>,
        dayRows: List<DayRow>,
        dayIndices: Set<Int>
    ): List<ImportedScheduleDraft> {
        val result = mutableListOf<ImportedScheduleDraft>()
        var currentDay = dayRows.firstOrNull()?.dayInfo ?: DayInfo.SATURDAY

        val timeRangePattern = Pattern.compile(
            "(\\d{1,2}[:.]\\d{2})\\s*(?:AM|PM|am|pm|ص|م)?\\s*(?:-|–|to|إلى)\\s*(\\d{1,2}[:.]\\d{2})\\s*(AM|PM|am|pm|ص|م)?",
            Pattern.CASE_INSENSITIVE
        )

        for ((idx, line) in allLines.withIndex()) {
            if (idx in dayIndices) {
                matchDay(line.text)?.let { currentDay = it }
                continue
            }
            if (isHeaderNoise(line.text) || isTrivialPlaceholder(line.text)) continue

            val matcher = timeRangePattern.matcher(line.text)
            if (matcher.find()) {
                val s = matcher.group(1)?.replace('.', ':') ?: "09:00"
                val e = matcher.group(2)?.replace('.', ':') ?: "10:00"
                val title = line.text.replace(matcher.group(0) ?: "", "")
                    .trim(' ', '-', ':', '•', '|', '[', ']', '(', ')')

                result.add(
                    ImportedScheduleDraft(
                        title = title.ifBlank { "محاضرة / جلسة دراسية" },
                        dayOfWeek = currentDay.calDay,
                        dayName = currentDay.arabicName,
                        timeText = "$s - $e",
                        startTime = s,
                        endTime = e,
                        durationMinutes = calculateDuration(s, e),
                        isFixed = true,
                        isWeeklyRecurring = true
                    )
                )
            } else if (line.text.length in 4..45 && !line.text.all { it.isDigit() || it.isWhitespace() }) {
                result.add(
                    ImportedScheduleDraft(
                        title = line.text.trim('•', '-', '|', ' '),
                        dayOfWeek = currentDay.calDay,
                        dayName = currentDay.arabicName,
                        timeText = "09:00 ص",
                        startTime = "09:00",
                        endTime = "10:00",
                        durationMinutes = 60,
                        isFixed = true,
                        isWeeklyRecurring = true
                    )
                )
            }
        }
        return result
    }

    private fun matchDay(text: String): DayInfo? {
        val clean = text.trim().lowercase()
        // Day match if word matches or starts with day name
        for (day in DayInfo.values()) {
            for (kw in day.keywords) {
                if (clean == kw || clean.startsWith("$kw ") || clean.startsWith("$kw-") || clean.startsWith("$kw/")) {
                    return day
                }
            }
        }
        return null
    }

    private fun parseHourHeader(text: String, rect: Rect): HourColumn? {
        val clean = text.trim()
        // Match e.g. "8-9", "08:00 - 09:00", "9-10", "10 - 11", "12-1", "1-2", "2-4", "2 - 3"
        val hourPattern = Pattern.compile(
            "^(\\d{1,2})(?:[:.](\\d{2}))?\\s*(?:-|–|to|إلى)\\s*(\\d{1,2})(?:[:.](\\d{2}))?$",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = hourPattern.matcher(clean)
        if (matcher.find()) {
            val sH = matcher.group(1)?.toIntOrNull() ?: return null
            val sM = matcher.group(2)?.toIntOrNull() ?: 0
            val eH = matcher.group(3)?.toIntOrNull() ?: return null
            val eM = matcher.group(4)?.toIntOrNull() ?: 0

            // Valid schedule hours (1 to 24)
            if (sH in 1..24 && eH in 1..24) {
                val sStr = String.format(Locale.US, "%02d:%02d", sH, sM)
                val eStr = String.format(Locale.US, "%02d:%02d", eH, eM)
                return HourColumn(
                    startHour = sH,
                    startMin = sM,
                    endHour = eH,
                    endMin = eM,
                    durationMinutes = 60,
                    formattedTime = "$sStr - $eStr",
                    startTimeStr = sStr,
                    endTimeStr = eStr,
                    xCenter = rect.centerX(),
                    rect = rect
                )
            }
        }
        return null
    }

    private fun formatTimeDisplay(sH: Int, sM: Int, eH: Int, eM: Int): String {
        val sPeriod = if (sH < 12) "ص" else "م"
        val ePeriod = if (eH < 12) "ص" else "م"
        val sDisplayHour = if (sH == 0 || sH == 12) 12 else sH % 12
        val eDisplayHour = if (eH == 0 || eH == 12) 12 else eH % 12

        return String.format(
            Locale.US,
            "%02d:%02d %s - %02d:%02d %s",
            sDisplayHour, sM, sPeriod,
            eDisplayHour, eM, ePeriod
        )
    }

    private fun isHeaderNoise(text: String): Boolean {
        val clean = text.trim().lowercase()
        val noiseExact = listOf(
            "day/hour", "day / hour", "day /hour", "day/ hour", "day", "hour", "time", "hours",
            "first week", "second week", "third week", "fourth week", "week", "first", "second",
            "last update", "last updated", "update", "updated", "date", "schedule", "timetable", "table",
            "faculty", "university", "department", "semester", "academic year",
            "اليوم", "الساعة", "الوقت", "الأسبوع", "الاسبوع", "الأول", "الثاني",
            "الجدول", "جامعة", "كلية", "قسم", "تحديث", "آخر تحديث", "الفصل الدراسي",
            "am", "pm", "ص", "م"
        )
        if (noiseExact.any { clean == it }) return true

        // Dates like 20/9/2026 or 20-09-2026 or 2026/09/20
        if (clean.matches(Regex("^\\d{1,4}[/.-]\\d{1,2}[/.-]\\d{1,4}$"))) return true

        // Check if starts with "last update" or "day/hour"
        if (clean.startsWith("last update") || clean.startsWith("day/hour") || clean.startsWith("faculty of")) return true

        // Day of week alone
        for (day in DayInfo.values()) {
            if (day.keywords.any { clean == it }) return true
        }

        // Pure numbers, slashes, punctuation only
        if (clean.matches(Regex("^[0-9\\s/.:\\-_|]+$"))) return true

        return false
    }

    private fun isTrivialPlaceholder(text: String): Boolean {
        val clean = text.trim().lowercase()
        val trivials = listOf(
            "-", "--", "---", "——", "—", "break", "lunch", "prayer", "off", "free",
            "استراحة", "راحة", "صلاة", "غداء", "فارغ", "لا يوجد"
        )
        return trivials.any { clean == it }
    }

    private fun calculateDuration(start: String, end: String): Int {
        return try {
            val sParts = start.split(":").map { it.trim().toInt() }
            val eParts = end.split(":").map { it.trim().toInt() }
            val diff = (eParts[0] * 60 + eParts.getOrElse(1) { 0 }) - (sParts[0] * 60 + sParts.getOrElse(1) { 0 })
            if (diff in 15..360) diff else 60
        } catch (_: Exception) {
            60
        }
    }
}
