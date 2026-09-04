package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BehavioralInsight
import com.example.model.InsightType
import com.example.model.ReviewPeriodTab
import com.example.model.ReviewStats
import com.example.ui.components.CircularProgressCard
import com.example.ui.components.FocusProgressBar
import com.example.ui.components.GlassCard
import com.example.ui.components.SegmentedTabRow
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.AmberTertiaryContainer
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryContainer
import com.example.ui.theme.SecondaryContainer
import com.example.ui.theme.SuccessSecondary
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceContainerHigh
import com.example.ui.theme.SurfaceSlateCard

@Composable
fun ReviewsScreen(
    stats: ReviewStats,
    activePeriod: ReviewPeriodTab,
    onPeriodChanged: (ReviewPeriodTab) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Section & Period Tabs (اليومي / الأسبوعي / الشهري)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "المراجعات والإحصائيات",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "تحليل الأداء الفعلي والتحفيز السلوكي الواقعي",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // TabRow (اليومي, الأسبوعي, الشهري)
                val tabs = ReviewPeriodTab.entries.map { it.title }
                SegmentedTabRow(
                    tabs = tabs,
                    selectedIndex = activePeriod.ordinal,
                    onTabSelected = { onPeriodChanged(ReviewPeriodTab.entries[it]) }
                )

                if (stats.totalCount == 0 && stats.completedCount == 0) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ابدأ بإنجاز أول جلسة تركيز لتظهر إحصائياتك هنا.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Animated Bento Grid of 4 Summary Cards based on Selected Period
        item {
            AnimatedContent(
                targetState = stats,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "stats_grid_anim"
            ) { targetStats ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Row 1 of 2 Cards: المهام المكتملة + إجمالي المهام
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Card 1: المهام المكتملة
                        SummaryStatCard(
                            title = "المهام المكتملة",
                            value = "${targetStats.completedCount}",
                            badge = targetStats.completedDelta,
                            badgeColor = SuccessSecondary,
                            icon = Icons.Default.TaskAlt,
                            iconColor = SuccessSecondary,
                            iconBg = SecondaryContainer.copy(alpha = 0.25f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_completed_tasks")
                        )

                        // Card 2: إجمالي المهام
                        SummaryStatCard(
                            title = "إجمالي المهام",
                            value = "${targetStats.totalCount}",
                            badge = targetStats.totalLabel,
                            badgeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Default.ListAlt,
                            iconColor = IndigoPrimary,
                            iconBg = IndigoPrimaryContainer.copy(alpha = 0.25f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_total_tasks")
                        )
                    }

                    // Row 2 of 2 Cards: ساعات التركيز + نسبة الإنجاز Ring
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Card 3: ساعات التركيز Logged
                        SummaryStatCard(
                            title = "ساعات التركيز",
                            value = targetStats.focusHours,
                            badge = when (activePeriod) {
                                ReviewPeriodTab.DAILY -> "اليوم"
                                ReviewPeriodTab.WEEKLY -> "الأسبوع"
                                ReviewPeriodTab.MONTHLY -> "الشهر"
                            },
                            badgeColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            icon = Icons.Default.Schedule,
                            iconColor = AmberTertiary,
                            iconBg = AmberTertiaryContainer.copy(alpha = 0.25f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_focus_hours")
                        )

                        // Card 4: نسبة الإنجاز with Circular Progress Ring
                        GlassCard(
                            modifier = Modifier
                                .weight(1f)
                                .height(148.dp)
                                .testTag("card_completion_ring"),
                            shape = RoundedCornerShape(16.dp),
                            backgroundColor = SurfaceSlateCard.copy(alpha = 0.85f),
                            borderColor = SurfaceCardBorder
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "نسبة الإنجاز",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                CircularProgressCard(
                                    percentage = targetStats.completionPercentage,
                                    modifier = Modifier.size(88.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Performance Trend Bar Card ("أداء الفترة")
        item {
            val periodTitle = when (activePeriod) {
                ReviewPeriodTab.DAILY -> "أداء اليوم"
                ReviewPeriodTab.WEEKLY -> "أداء الأسبوع"
                ReviewPeriodTab.MONTHLY -> "أداء الشهر"
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("performance_card"),
                shape = RoundedCornerShape(18.dp),
                backgroundColor = SurfaceSlateCard.copy(alpha = 0.85f),
                borderColor = SurfaceCardBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = periodTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stats.performanceTitle,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stats.completionPercentage}%",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = IndigoPrimary
                        )
                    }

                    FocusProgressBar(
                        progress = stats.performanceProgress,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                        progressColor = if (stats.completionPercentage >= 70) SuccessSecondary else IndigoPrimary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (stats.completionPercentage >= 70) SuccessSecondary else IndigoPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stats.performanceMessage,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            color = if (stats.completionPercentage >= 70) SuccessSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Behavioral Motivation Insight Card (Offline Motivation Engine)
        item {
            BehavioralInsightCard(
                insight = stats.behavioralInsight,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("behavioral_insight_card")
                    .testTag("motivational_quote_card")
            )
        }
    }
}

@Composable
fun BehavioralInsightCard(
    insight: BehavioralInsight,
    modifier: Modifier = Modifier
) {
    val accentColor = when (insight.insightType) {
        InsightType.HIGH_EXECUTION -> SuccessSecondary
        InsightType.OVERLOADED_DAY -> AmberTertiary
        InsightType.REPEATED_POSTPONEMENT -> AmberTertiary
        InsightType.MOMENTUM_RECOVERY -> IndigoPrimary
        InsightType.BALANCED_PROGRESS, InsightType.CONSISTENCY -> IndigoPrimary
    }

    val iconVector: ImageVector = when (insight.insightType) {
        InsightType.HIGH_EXECUTION -> Icons.Default.Celebration
        InsightType.OVERLOADED_DAY -> Icons.Default.Tune
        InsightType.REPEATED_POSTPONEMENT -> Icons.Default.Schedule
        InsightType.MOMENTUM_RECOVERY -> Icons.Default.TrendingUp
        InsightType.BALANCED_PROGRESS, InsightType.CONSISTENCY -> Icons.Default.Psychology
    }

    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        backgroundColor = SurfaceContainerHigh.copy(alpha = 0.95f),
        borderColor = accentColor.copy(alpha = 0.35f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Background subtle watermark quote icon
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(76.dp)
                    .align(Alignment.TopEnd)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Badge & Category
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = insight.categoryName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = accentColor
                    )
                }

                // Insight Title
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Insight Message
                Text(
                    text = "\"${insight.message}\"",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
                )
            }
        }
    }
}

@Composable
fun SummaryStatCard(
    title: String,
    value: String,
    badge: String,
    badgeColor: Color,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.height(148.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = SurfaceSlateCard.copy(alpha = 0.85f),
        borderColor = SurfaceCardBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = badge,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = badgeColor
                )
            }
        }
    }
}
