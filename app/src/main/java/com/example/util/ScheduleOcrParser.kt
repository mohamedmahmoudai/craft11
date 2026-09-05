package com.example.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ImportedScheduleDraft(
    val id: String = java.util.UUID.randomUUID().toString(),
    var title: String,
    var timeText: String,
    var durationMinutes: Int = 60,
    var isFixed: Boolean = true,
    var isSelected: Boolean = true
)

object ScheduleOcrParser {

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
     * Parses ML Kit Vision Text into structured schedule draft items
     */
    fun parseScheduleItems(visionText: Text): List<ImportedScheduleDraft> {
        val result = mutableListOf<ImportedScheduleDraft>()
        val rawText = visionText.text

        if (rawText.isBlank()) return emptyList()

        // Regex patterns to detect times (e.g. 09:00, 10:30 AM, 2:15 PM, 08:00 - 09:30, 8:00 ص)
        val timeRangePattern = Pattern.compile(
            "(\\d{1,2}[:.]\\d{2})\\s*(?:AM|PM|am|pm|ص|م)?\\s*(?:-|–|to|إلى)\\s*(\\d{1,2}[:.]\\d{2})\\s*(AM|PM|am|pm|ص|م)?",
            Pattern.CASE_INSENSITIVE
        )
        val singleTimePattern = Pattern.compile(
            "(\\d{1,2}[:.]\\d{2})\\s*(AM|PM|am|pm|ص|م)?",
            Pattern.CASE_INSENSITIVE
        )

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val defaultStartHours = listOf(8, 9, 10, 11, 13, 14, 16, 18, 20)
        var defaultIndex = 0

        for (line in lines) {
            // Ignore trivial header noise like numbers only, page numbers, or short words
            if (line.length < 3) continue

            val rangeMatcher = timeRangePattern.matcher(line)
            if (rangeMatcher.find()) {
                val start = rangeMatcher.group(1)?.replace('.', ':') ?: "09:00"
                val end = rangeMatcher.group(2)?.replace('.', ':') ?: "10:00"
                val period = rangeMatcher.group(3) ?: ""
                val cleanTitle = line.replace(rangeMatcher.group(0) ?: "", "").trim(' ', '-', ':', '•', '|')

                val formattedTime = if (period.isNotBlank()) "$start $period" else "$start - $end"
                val duration = calculateDuration(start, end)

                result.add(
                    ImportedScheduleDraft(
                        title = cleanTitle.ifBlank { "فترة دراسية / موعد" },
                        timeText = formattedTime,
                        durationMinutes = duration,
                        isFixed = true
                    )
                )
                continue
            }

            val singleMatcher = singleTimePattern.matcher(line)
            if (singleMatcher.find()) {
                val time = singleMatcher.group(1)?.replace('.', ':') ?: "09:00"
                val period = singleMatcher.group(2) ?: ""
                val cleanTitle = line.replace(singleMatcher.group(0) ?: "", "").trim(' ', '-', ':', '•', '|')
                val formattedTime = if (period.isNotBlank()) "$time $period" else "$time ص"

                result.add(
                    ImportedScheduleDraft(
                        title = cleanTitle.ifBlank { "مهمة مجدولة" },
                        timeText = formattedTime,
                        durationMinutes = 60,
                        isFixed = true
                    )
                )
                continue
            }

            // If line contains meaningful schedule text (e.g. subject or activity name)
            if (line.length in 4..40 && !line.all { it.isDigit() || it.isWhitespace() }) {
                val hour = defaultStartHours.getOrElse(defaultIndex % defaultStartHours.size) { 10 }
                val timeStr = if (hour < 12) String.format(java.util.Locale.US, "%02d:00 ص", hour)
                else String.format(java.util.Locale.US, "%02d:00 م", if (hour == 12) 12 else hour - 12)

                defaultIndex++
                result.add(
                    ImportedScheduleDraft(
                        title = line.trim('•', '-', '|', ' '),
                        timeText = timeStr,
                        durationMinutes = 45,
                        isFixed = false
                    )
                )
            }
        }

        return result
    }

    private fun calculateDuration(start: String, end: String): Int {
        return try {
            val startParts = start.split(":").map { it.trim().toInt() }
            val endParts = end.split(":").map { it.trim().toInt() }
            val startMins = startParts[0] * 60 + startParts.getOrElse(1) { 0 }
            val endMins = endParts[0] * 60 + endParts.getOrElse(1) { 0 }
            val diff = endMins - startMins
            if (diff > 0 && diff <= 360) diff else 60
        } catch (_: Exception) {
            60
        }
    }
}
