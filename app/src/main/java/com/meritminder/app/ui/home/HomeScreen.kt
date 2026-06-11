package com.meritminder.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.R
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.PracticeWithGoals
import com.meritminder.app.data.remote.Group
import com.meritminder.app.data.remote.GroupStatus
import com.meritminder.app.ui.theme.DoneGreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
    onOpenCounter: (Int) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val practices by viewModel.practicesWithGoals.collectAsState()
    val todayRecords by viewModel.todayRecords.collectAsState()
    val allTotals by viewModel.allTotals.collectAsState()
    val summary by viewModel.todaySummary.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val groupStatuses by viewModel.groupStatuses.collectAsState()
    var logDialogTarget by remember { mutableStateOf<PracticeWithGoals?>(null) }
    var checkInGroupTarget by remember { mutableStateOf<GroupStatus?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    if (syncing) {
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.syncing), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val (donePractices, pendingPractices) = remember(practices, todayRecords, allTotals) {
        practices.partition { pwg ->
            val goal = pwg.goals.firstOrNull { it.isActive } ?: return@partition false
            val todayValue = todayRecords.find { it.practiceId == pwg.practice.id }?.completedValue ?: 0L
            val totalValue = allTotals[pwg.practice.id] ?: 0L
            isDoneToday(goal, todayValue, totalValue)
        }
    }

    val user = FirebaseAuth.getInstance().currentUser
    val displayName = user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@") ?: "修行者"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            TodayHeader(
                userName = displayName,
                today = viewModel.today,
                summary = summary,
                streak = streak
            )
        }

        if (practices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillParentMaxWidth().padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_practices),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@LazyColumn
        }

        if (pendingPractices.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.section_pending))
                Text(
                    text = "长按功课名可启动计数器",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
                )
            }
            items(pendingPractices, key = { it.practice.id }) { pwg ->
                val goal = pwg.goals.firstOrNull { it.isActive }
                val todayValue = todayRecords.find { it.practiceId == pwg.practice.id }?.completedValue ?: 0L
                val totalValue = allTotals[pwg.practice.id] ?: 0L
                PracticeRow(
                    pwg = pwg,
                    goal = goal,
                    todayValue = todayValue,
                    totalValue = totalValue,
                    isDone = false,
                    onCircleClick = {
                        if (goal?.targetType == Goal.TYPE_CHECKIN) {
                            viewModel.toggleCheckin(pwg.practice.id)
                        } else {
                            logDialogTarget = pwg
                        }
                    },
                    onOpenCounter = if (goal?.targetType != Goal.TYPE_CHECKIN) {
                        { onOpenCounter(pwg.practice.id) }
                    } else null
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp, end = 20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        if (donePractices.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_completed)) }
            items(donePractices, key = { it.practice.id }) { pwg ->
                val goal = pwg.goals.firstOrNull { it.isActive }
                val todayValue = todayRecords.find { it.practiceId == pwg.practice.id }?.completedValue ?: 0L
                val totalValue = allTotals[pwg.practice.id] ?: 0L
                PracticeRow(
                    pwg = pwg,
                    goal = goal,
                    todayValue = todayValue,
                    totalValue = totalValue,
                    isDone = true,
                    onCircleClick = {
                        if (goal?.targetType == Goal.TYPE_CHECKIN) {
                            viewModel.toggleCheckin(pwg.practice.id)
                        } else {
                            logDialogTarget = pwg
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp, end = 20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        if (groupStatuses.isNotEmpty()) {
            item { SectionHeader("共修功课") }
            items(groupStatuses, key = { "group_${it.group.id}" }) { status ->
                GroupPracticeRow(
                    status = status,
                    onCheckIn = { checkInGroupTarget = status }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp, end = 20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }

    logDialogTarget?.let { target ->
        LogProgressDialog(
            practiceName = target.practice.name,
            onConfirm = { value ->
                viewModel.logProgress(target.practice.id, value)
                logDialogTarget = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.snackbar_logged),
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDismiss = { logDialogTarget = null }
        )
    }

    checkInGroupTarget?.let { status ->
        GroupCheckInDialog(
            practiceName = status.group.practiceName,
            groupName = status.group.name,
            onConfirm = { value ->
                viewModel.checkInGroup(status.group.id, value)
                checkInGroupTarget = null
            },
            onDismiss = { checkInGroupTarget = null }
        )
    }
}

private fun isDoneToday(goal: Goal, todayValue: Long, totalValue: Long): Boolean = when {
    goal.targetType == Goal.TYPE_CHECKIN -> todayValue >= 1L
    goal.periodType == Goal.PERIOD_DAILY -> goal.targetValue > 0 && todayValue >= goal.targetValue
    goal.deadlineDate != null -> {
        val daysLeft = maxOf(1L, ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(goal.deadlineDate)) + 1)
        val remaining = maxOf(0L, goal.targetValue - totalValue)
        val daily = (remaining + daysLeft - 1) / daysLeft
        todayValue >= daily
    }
    else -> goal.targetValue > 0 && totalValue >= goal.targetValue
}

@Composable
private fun TodayHeader(userName: String, today: String, summary: TodaySummary?, streak: Int) {
    val date = LocalDate.parse(today)
    val locale = Locale.getDefault()
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val dateStr = if (locale.language == "zh") {
        "${date.year}年${date.monthValue}月${date.dayOfMonth}日 $dayOfWeek"
    } else {
        "$dayOfWeek, ${date.month.getDisplayName(TextStyle.FULL, locale)} ${date.dayOfMonth}"
    }
    val initial = userName.firstOrNull()?.toString() ?: "?"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(
                label = stringResource(R.string.stat_today_label),
                value = summary?.let { "${it.completed} / ${it.total}" } ?: "—",
                modifier = Modifier.weight(1f)
            )
            StatChip(
                label = stringResource(R.string.stat_streak_label),
                value = stringResource(R.string.stat_streak_days, streak),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = valueColor
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PracticeRow(
    pwg: PracticeWithGoals,
    goal: Goal?,
    todayValue: Long,
    totalValue: Long,
    isDone: Boolean,
    onCircleClick: () -> Unit,
    onOpenCounter: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .then(
                    if (onOpenCounter != null) Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
                    else Modifier
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
        CircleCheckbox(isDone = isDone, onClick = onCircleClick)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pwg.practice.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )
            if (goal != null) {
                val subtitle = practiceSubtitle(goal, totalValue)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        if (goal != null) {
            PracticeProgress(
                goal = goal,
                todayValue = todayValue,
                totalValue = totalValue,
                isDone = isDone
            )
        }
        }

        if (onOpenCounter != null) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("计数器") },
                    onClick = {
                        showMenu = false
                        onOpenCounter()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.TouchApp, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
private fun CircleCheckbox(isDone: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (isDone) Modifier.background(DoneGreen)
                else Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun PracticeProgress(goal: Goal, todayValue: Long, totalValue: Long, isDone: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    when {
        isDone -> Text(
            text = stringResource(R.string.progress_done),
            style = MaterialTheme.typography.bodySmall,
            color = DoneGreen
        )
        goal.targetType == Goal.TYPE_CHECKIN -> Text(
            text = stringResource(R.string.progress_not_done),
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
        goal.deadlineDate != null && goal.periodType == Goal.PERIOD_LONG_TERM -> {
            val deadline = LocalDate.parse(goal.deadlineDate)
            val daysLeft = maxOf(1L, ChronoUnit.DAYS.between(LocalDate.now(), deadline) + 1)
            val remaining = maxOf(0L, goal.targetValue - totalValue)
            val daily = (remaining + daysLeft - 1) / daysLeft
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (remaining == 0L) "已完成" else "今日需 $daily 遍",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining == 0L) DoneGreen else primary
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        if (goal.targetValue > 0) (totalValue.toFloat() / goal.targetValue).coerceIn(0f, 1f) else 0f
                    },
                    modifier = Modifier.width(60.dp),
                    color = if (remaining == 0L) DoneGreen else primary
                )
            }
        }
        goal.targetType == Goal.TYPE_COURSE -> {
            val pct = if (goal.targetValue > 0) (totalValue * 100 / goal.targetValue).toInt() else 0
            Column(horizontalAlignment = Alignment.End) {
                Text("$pct%", style = MaterialTheme.typography.bodySmall, color = primary)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = {
                        if (goal.targetValue > 0) (totalValue.toFloat() / goal.targetValue).coerceIn(0f, 1f) else 0f
                    },
                    modifier = Modifier.width(60.dp),
                    color = primary
                )
            }
        }
        goal.periodType == Goal.PERIOD_DAILY -> Text(
            text = "$todayValue / ${goal.targetValue}",
            style = MaterialTheme.typography.bodyMedium,
            color = primary
        )
        else -> Text(
            text = stringResource(R.string.total_recitations, totalValue),
            style = MaterialTheme.typography.bodySmall,
            color = muted
        )
    }
}

@Composable
private fun practiceSubtitle(goal: Goal, totalValue: Long): String {
    if (goal.deadlineDate != null && goal.periodType == Goal.PERIOD_LONG_TERM) {
        val deadline = LocalDate.parse(goal.deadlineDate)
        val daysLeft = maxOf(1L, ChronoUnit.DAYS.between(LocalDate.now(), deadline) + 1)
        val remaining = maxOf(0L, goal.targetValue - totalValue)
        val daily = (remaining + daysLeft - 1) / daysLeft
        return "限期完成 · 今日需 $daily 遍 · 还剩 $daysLeft 天"
    }
    return when (goal.targetType) {
        Goal.TYPE_CHECKIN -> stringResource(R.string.subtitle_checkin)
        Goal.TYPE_COURSE  -> stringResource(R.string.subtitle_course, totalValue, goal.targetValue)
        Goal.TYPE_TIME    -> if (goal.periodType == Goal.PERIOD_DAILY)
            stringResource(R.string.subtitle_daily_minutes, goal.targetValue)
            else stringResource(R.string.subtitle_lifetime)
        else -> if (goal.periodType == Goal.PERIOD_DAILY)
            stringResource(R.string.subtitle_daily_count, goal.targetValue)
            else stringResource(R.string.subtitle_lifetime)
    }
}

@Composable
private fun LogProgressDialog(
    practiceName: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(practiceName) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.log_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(input.toLongOrNull() ?: 0L) },
                enabled = input.isNotBlank()
            ) { Text(stringResource(R.string.btn_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

@Composable
private fun GroupPracticeRow(status: GroupStatus, onCheckIn: () -> Unit) {
    val doneToday = status.myDoneToday
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleCheckbox(isDone = doneToday, onClick = onCheckIn)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = status.group.practiceName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "小组：${status.group.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (status.group.targetType == Group.TYPE_TOTAL) {
                val fraction = if (status.group.targetValue > 0)
                    (status.myTotal.toFloat() / status.group.targetValue).coerceIn(0f, 1f) else 0f
                Text(
                    text = "${status.myTotal} / ${status.group.targetValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.width(60.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            } else {
                val fraction = if (status.group.targetValue > 0)
                    (status.myTodayValue.toFloat() / status.group.targetValue).coerceIn(0f, 1f) else 0f
                Text(
                    text = "${status.myTodayValue} / ${status.group.targetValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (doneToday) DoneGreen else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.width(60.dp),
                    color = if (doneToday) DoneGreen else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GroupCheckInDialog(
    practiceName: String,
    groupName: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(practiceName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "小组：$groupName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() } },
                    label = { Text("今日完成数量（遍）") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount.toLongOrNull() ?: 0L) },
                enabled = (amount.toLongOrNull() ?: 0L) > 0L
            ) { Text(stringResource(R.string.btn_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
