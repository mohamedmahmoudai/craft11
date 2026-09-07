package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.WhackaAmber
import com.example.ui.theme.WhackaEmerald
import com.example.ui.theme.WhackaPrimaryAccent
import com.example.ui.theme.WhackaRed
import com.example.util.ImportedScheduleDraft

@Composable
fun ImportScheduleReviewDialog(
    initialDrafts: List<ImportedScheduleDraft>,
    onDismiss: () -> Unit,
    onConfirmImport: (List<ImportedScheduleDraft>) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember { mutableStateListOf<ImportedScheduleDraft>().apply { addAll(initialDrafts) } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .imePadding()
                .testTag("ocr_schedule_review_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "مراجعة الجدول الدراسي الذكي",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = WhackaPrimaryAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "تم التوزيع حسب أيام الأسبوع والمصفوفة الزمنية",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = WhackaPrimaryAccent
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    // Bulk selection controls
                    if (items.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allSelected = items.all { it.isSelected }
                            TextButton(
                                onClick = {
                                    val newSelect = !allSelected
                                    items.forEachIndexed { i, draft ->
                                        items[i] = draft.copy(isSelected = newSelect)
                                    }
                                }
                            ) {
                                Text(
                                    text = if (allSelected) "إلغاء تحديد الكل" else "تحديد الكل",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WhackaPrimaryAccent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Text(
                                text = "${items.count { it.isSelected }} من ${items.size} مادة محددة",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (items.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = WhackaAmber.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, WhackaAmber.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "لم نتمكن من قراءة مواد دراسية واضحة من الجدول. يرجى التأكد من وضوح الصورة وزاوية التصوير.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Scrollable List of Parsed Items
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 390.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (item.isSelected) WhackaPrimaryAccent.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Header Row: Checkbox, Day Badge, and Delete
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Checkbox(
                                                    checked = item.isSelected,
                                                    onCheckedChange = { isChecked ->
                                                        items[index] = item.copy(isSelected = isChecked)
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = WhackaPrimaryAccent)
                                                )

                                                // Day Pill Badge
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = WhackaPrimaryAccent.copy(alpha = 0.15f),
                                                    border = BorderStroke(1.dp, WhackaPrimaryAccent.copy(alpha = 0.4f))
                                                ) {
                                                    Text(
                                                        text = item.dayName,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = WhackaPrimaryAccent
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { items.removeAt(index) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف",
                                                    tint = WhackaRed,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Subject Title
                                        OutlinedTextField(
                                            value = item.title,
                                            onValueChange = { newTitle ->
                                                items[index] = item.copy(title = newTitle)
                                            },
                                            label = { Text("اسم المادة / المحاضرة", fontSize = 11.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Notes / Location / Section
                                        if (item.subtitle.isNotBlank() || item.isSelected) {
                                            OutlinedTextField(
                                                value = item.subtitle,
                                                onValueChange = { newSub ->
                                                    items[index] = item.copy(subtitle = newSub)
                                                },
                                                label = { Text("الموقع / القاعة / الملاحظات", fontSize = 11.sp) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        // Time, Duration Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = item.timeText,
                                                onValueChange = { newTime ->
                                                    items[index] = item.copy(timeText = newTime)
                                                },
                                                label = { Text("التوقيت", fontSize = 11.sp) },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                modifier = Modifier.weight(1.3f)
                                            )

                                            OutlinedTextField(
                                                value = "${item.durationMinutes}",
                                                onValueChange = { newDur ->
                                                    val parsed = newDur.filter { it.isDigit() }.toIntOrNull() ?: 60
                                                    items[index] = item.copy(durationMinutes = parsed)
                                                },
                                                label = { Text("المدة (د)", fontSize = 11.sp) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                modifier = Modifier.weight(0.8f)
                                            )
                                        }

                                        // Toggles: Weekly Recurring (Enabled by default) & Fixed
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Weekly recurring toggle (ENABLED BY DEFAULT)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        items[index] = item.copy(isWeeklyRecurring = !item.isWeeklyRecurring)
                                                    }
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Switch(
                                                    checked = item.isWeeklyRecurring,
                                                    onCheckedChange = { recurring ->
                                                        items[index] = item.copy(isWeeklyRecurring = recurring)
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = WhackaPrimaryAccent
                                                    ),
                                                    modifier = Modifier.testTag("weekly_recurring_switch_${index}")
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.EventRepeat,
                                                        contentDescription = null,
                                                        tint = WhackaPrimaryAccent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "تكرار أسبوعي",
                                                        style = MaterialTheme.typography.bodySmall.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    )
                                                }
                                            }

                                            // Fixed vs Flexible toggle
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        items[index] = item.copy(isFixed = !item.isFixed)
                                                    }
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                Switch(
                                                    checked = item.isFixed,
                                                    onCheckedChange = { fixed ->
                                                        items[index] = item.copy(isFixed = fixed)
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = WhackaEmerald
                                                    )
                                                )
                                                Text(
                                                    text = if (item.isFixed) "موعد ثابت" else "مرن",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Actions
                    val selectedCount = items.count { it.isSelected && it.title.isNotBlank() }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("إلغاء", style = MaterialTheme.typography.bodyMedium)
                        }

                        Button(
                            onClick = {
                                val selectedItems = items.filter { it.isSelected && it.title.isNotBlank() }
                                onConfirmImport(selectedItems)
                            },
                            enabled = selectedCount > 0,
                            modifier = Modifier
                                .weight(2f)
                                .height(48.dp)
                                .testTag("confirm_ocr_import_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "توزيع المواد في الجدول ($selectedCount)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
