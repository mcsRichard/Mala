package com.meritminder.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.PracticeWithGoals

@Composable
fun StatisticsScreen(contentPadding: PaddingValues) {
    val vm: StatisticsViewModel = viewModel()
    val pwgs by vm.practicesWithGoals.collectAsState()
    val allTotals by vm.allTotals.collectAsState()
    val totalActiveDays by vm.totalActiveDays.collectAsState()
    val last7Days by vm.last7Days.collectAsState()
    val streak by vm.streak.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        item { OverviewSection(pwgs.size, totalActiveDays, streak) }
        item { WeeklySection(last7Days) }
        if (pwgs.isNotEmpty()) {
            item { SectionLabel("各功课统计") }
            items(pwgs) { pwg ->
                PracticeStatRow(pwg, allTotals[pwg.practice.id] ?: 0L)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun OverviewSection(totalPractices: Int, activeDays: Int, streak: Int) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)) {
        SectionLabel("概览", topPadding = false)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OverviewCard("总功课", "$totalPractices 个", Modifier.weight(1f))
            OverviewCard("打卡天数", "$activeDays 天", Modifier.weight(1f))
            OverviewCard("当前连续", "$streak 天", Modifier.weight(1f))
        }
    }
}

@Composable
private fun OverviewCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklySection(days: List<DayBarData>) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
    ) {
        SectionLabel("近7日完成", topPadding = false)
        if (days.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    days.forEach { DayBar(it) }
                }
            }
        }
    }
}

@Composable
private fun DayBar(day: DayBarData) {
    val primary = MaterialTheme.colorScheme.primary
    val barColor = when {
        day.fraction >= 1f -> primary
        day.fraction > 0f -> primary.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val labelColor = if (day.isToday) primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(38.dp)
    ) {
        Text(
            text = if (day.total > 0) "${day.completed}/${day.total}" else "-",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .height(90.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            val fill = if (day.total == 0) 0.05f else day.fraction.coerceAtLeast(0.05f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(fill)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun PracticeStatRow(pwg: PracticeWithGoals, total: Long) {
    val goal = pwg.goals.firstOrNull()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pwg.practice.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (goal != null) {
                Text(
                    text = goalTypeLabel(goal),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        if (goal != null) {
            when {
                goal.targetType == Goal.TYPE_CHECKIN -> {
                    Text(
                        text = "打卡 $total 天",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                goal.targetType == Goal.TYPE_COURSE -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$total / ${goal.targetValue} 课",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = {
                                if (goal.targetValue > 0)
                                    (total.toFloat() / goal.targetValue).coerceIn(0f, 1f)
                                else 0f
                            },
                            modifier = Modifier.width(80.dp).padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                goal.deadlineDate != null -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$total / ${goal.targetValue} 遍",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = {
                                if (goal.targetValue > 0)
                                    (total.toFloat() / goal.targetValue).coerceIn(0f, 1f)
                                else 0f
                            },
                            modifier = Modifier.width(80.dp).padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
                else -> {
                    Text(
                        text = "累计 $total 遍",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, topPadding: Boolean = true) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(
            top = if (topPadding) 20.dp else 0.dp,
            bottom = 12.dp
        )
    )
}

private fun goalTypeLabel(goal: Goal): String = when {
    goal.targetType == Goal.TYPE_CHECKIN -> "每日打卡"
    goal.targetType == Goal.TYPE_COURSE -> "课程进度"
    goal.deadlineDate != null -> "限期完成"
    goal.periodType == Goal.PERIOD_LONG_TERM -> "终生累计"
    else -> "每日数量"
}
