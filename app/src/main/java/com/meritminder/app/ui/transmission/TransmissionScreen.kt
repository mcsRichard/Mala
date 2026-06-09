package com.meritminder.app.ui.transmission

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meritminder.app.data.local.entity.Transmission
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransmissionScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransmissionViewModel = viewModel()
) {
    val transmissions by viewModel.transmissions.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Transmission?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("传承灌顶") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editTarget = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        if (transmissions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "点击 + 添加传承或灌顶记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(transmissions, key = { it.id }) { t ->
                    TransmissionItem(
                        transmission = t,
                        onClick = { editTarget = t; showDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
    }

    if (showDialog) {
        TransmissionDialog(
            initial = editTarget,
            onSave = { name, teacher, date, place, notes ->
                val target = editTarget
                if (target != null) viewModel.update(target, name, teacher, date, place, notes)
                else viewModel.add(name, teacher, date, place, notes)
                showDialog = false
            },
            onDelete = editTarget?.let { t -> { viewModel.delete(t); showDialog = false } },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TransmissionItem(transmission: Transmission, onClick: () -> Unit) {
    val subtitle = listOfNotNull(
        transmission.teacher.ifBlank { null }?.let { "上师：$it" },
        transmission.date.ifBlank { null }?.let { formatDate(it) },
        transmission.place.ifBlank { null }?.let { "地点：$it" }
    ).joinToString(" · ")

    ListItem(
        headlineContent = {
            Text(transmission.name, fontWeight = FontWeight.Medium)
        },
        supportingContent = {
            Column {
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
                if (transmission.notes.isNotBlank()) {
                    Text(
                        transmission.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransmissionDialog(
    initial: Transmission?,
    onSave: (name: String, teacher: String, date: String, place: String, notes: String) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var teacher by rememberSaveable { mutableStateOf(initial?.teacher ?: "") }
    var date by rememberSaveable { mutableStateOf(initial?.date ?: "") }
    var place by rememberSaveable { mutableStateOf(initial?.place ?: "") }
    var notes by rememberSaveable { mutableStateOf(initial?.notes ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isValid = name.isNotBlank() && teacher.isNotBlank() && date.isNotBlank() && place.isNotBlank()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.ifBlank { null }
                ?.let { runCatching { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull() }
                ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除记录") },
            text = { Text("确定要删除「${initial?.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete?.invoke() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial != null) "编辑记录" else "添加传承 / 灌顶") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("传承 / 灌顶名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { Text("上师 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(if (date.isBlank()) "选择时间 *" else formatDate(date))
                }
                OutlinedTextField(
                    value = place,
                    onValueChange = { place = it },
                    label = { Text("地点 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, teacher, date, place, notes) },
                enabled = isValid
            ) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

private fun formatDate(date: String): String = try {
    val ld = LocalDate.parse(date)
    ld.format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
} catch (_: Exception) { date }
