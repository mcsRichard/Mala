package com.meritminder.app.ui.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meritminder.app.R
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.Practice
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPracticeScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddPracticeViewModel = viewModel()
) {
    val saved by viewModel.saved.collectAsState()
    val loadedData by viewModel.loadedData.collectAsState()

    var name by rememberSaveable { mutableStateOf(viewModel.initialName) }
    var practiceType by rememberSaveable { mutableStateOf(Practice.TYPE_CHANTING) }
    var goalPreset by rememberSaveable { mutableStateOf(AddPracticeViewModel.PRESET_DAILY_COUNT) }
    var targetValue by rememberSaveable { mutableStateOf("") }
    var deadlineDate by rememberSaveable { mutableStateOf<String?>(null) }
    var populated by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loadedData) {
        if (!populated && loadedData != null) {
            val practice = loadedData!!.practice
            val goal = loadedData!!.goals.firstOrNull { it.isActive }
            name = practice.name
            practiceType = practice.type
            if (goal != null) {
                goalPreset = AddPracticeViewModel.goalToPreset(goal)
                targetValue = if (goal.targetType == Goal.TYPE_CHECKIN) "" else goal.targetValue.toString()
                deadlineDate = goal.deadlineDate
            }
            populated = true
        }
    }

    LaunchedEffect(saved) {
        if (saved) { onNavigateBack(); viewModel.resetSaved() }
    }

    val needsTarget = goalPreset != AddPracticeViewModel.PRESET_CHECKIN
    val isDeadline = goalPreset == AddPracticeViewModel.PRESET_DEADLINE
    val isValid = name.isNotBlank() &&
        (!needsTarget || targetValue.isNotBlank()) &&
        (!isDeadline || deadlineDate != null)

    // Deadline preview calculation
    val daysLeft: Long? = if (isDeadline && deadlineDate != null) {
        maxOf(1L, ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(deadlineDate)) + 1)
    } else null
    val tvLong = targetValue.toLongOrNull() ?: 0L
    val dailyPreview: Long? = if (daysLeft != null && tvLong > 0) {
        (tvLong + daysLeft - 1) / daysLeft
    } else null

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadlineDate
                ?.let { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
                ?: (System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        deadlineDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (viewModel.isEditMode) stringResource(R.string.edit_practice_title)
                         else stringResource(R.string.add_practice_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.practice_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column {
                Text(stringResource(R.string.practice_type), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Practice.TYPE_CHANTING   to stringResource(R.string.type_chanting),
                        Practice.TYPE_MEDITATION to stringResource(R.string.type_meditation),
                        Practice.TYPE_OTHER      to stringResource(R.string.type_other)
                    ).forEach { (value, label) ->
                        FilterChip(selected = practiceType == value, onClick = { practiceType = value }, label = { Text(label) })
                    }
                }
            }

            Column {
                Text(stringResource(R.string.goal_type), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AddPracticeViewModel.PRESET_DAILY_COUNT to stringResource(R.string.goal_daily_count),
                        AddPracticeViewModel.PRESET_CHECKIN     to stringResource(R.string.goal_checkin),
                        AddPracticeViewModel.PRESET_LIFETIME    to stringResource(R.string.goal_lifetime),
                        AddPracticeViewModel.PRESET_COURSE      to stringResource(R.string.goal_course),
                        AddPracticeViewModel.PRESET_DEADLINE    to "限期完成"
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = goalPreset == value,
                            onClick = {
                                goalPreset = value
                                if (value == AddPracticeViewModel.PRESET_CHECKIN) targetValue = ""
                                if (value != AddPracticeViewModel.PRESET_DEADLINE) deadlineDate = null
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (needsTarget) {
                val hint = when (goalPreset) {
                    AddPracticeViewModel.PRESET_COURSE   -> stringResource(R.string.target_lessons_hint)
                    AddPracticeViewModel.PRESET_DEADLINE -> "总目标数量（遍）"
                    else                                 -> stringResource(R.string.target_count_hint)
                }
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it.filter { c -> c.isDigit() } },
                    label = { Text(hint) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            if (isDeadline) {
                val dateLabel = deadlineDate
                    ?.let { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("yyyy年M月d日")) }
                    ?: "选择截止日期"
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(dateLabel)
                }

                if (dailyPreview != null && daysLeft != null) {
                    Text(
                        text = "距截止日还有 $daysLeft 天，每日需完成 $dailyPreview 遍",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val tv = targetValue.toLongOrNull() ?: 0L
                    if (viewModel.isEditMode)
                        viewModel.updatePractice(name, practiceType, goalPreset, tv, deadlineDate)
                    else
                        viewModel.addPractice(name, practiceType, goalPreset, tv, deadlineDate)
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
