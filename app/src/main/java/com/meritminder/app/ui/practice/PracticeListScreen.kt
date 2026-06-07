package com.meritminder.app.ui.practice

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meritminder.app.R
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.PracticeWithGoals
import com.meritminder.app.ui.home.HomeViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PracticeListScreen(
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = viewModel()
) {
    val practices by viewModel.practicesWithGoals.collectAsState()
    val items = remember(practices) { practices.toMutableStateList() }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }

    if (practices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_practices),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        item {
            Text(
                text = stringResource(R.string.manage_practices_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
            HorizontalDivider()
        }

        itemsIndexed(items, key = { _, item -> item.practice.id }) { index, pwg ->
            val isDragging = index == draggingIndex

            DraggableRow(
                pwg = pwg,
                isDragging = isDragging,
                dragOffsetY = if (isDragging) draggingOffsetY else 0f,
                modifier = Modifier
                    .animateItemPlacement()
                    .onSizeChanged { size ->
                        if (itemHeightPx == 0f) itemHeightPx = size.height.toFloat()
                    },
                onDragStart = {
                    draggingIndex = index
                    draggingOffsetY = 0f
                },
                onDrag = { deltaY ->
                    val current = draggingIndex ?: return@DraggableRow
                    draggingOffsetY += deltaY
                    val steps = (draggingOffsetY / itemHeightPx).roundToInt()
                    val target = (current + steps).coerceIn(0, items.size - 1)
                    if (target != current) {
                        moveItem(items, current, target)
                        draggingOffsetY -= (target - current) * itemHeightPx
                        draggingIndex = target
                    }
                },
                onDragEnd = {
                    draggingIndex = null
                    draggingOffsetY = 0f
                    viewModel.updateSortOrder(items.map { it.practice })
                },
                onEditTarget = { newTarget -> viewModel.updateGoalTarget(pwg.practice.id, newTarget) },
                onDelete = { viewModel.deletePractice(pwg.practice) }
            )
        }
    }
}

@Composable
private fun DraggableRow(
    pwg: PracticeWithGoals,
    isDragging: Boolean,
    dragOffsetY: Float,
    modifier: Modifier = Modifier,
    onDragStart: () -> Unit,
    onDrag: (deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
    onEditTarget: (Long) -> Unit,
    onDelete: () -> Unit
) {
    val goal = pwg.goals.firstOrNull { it.isActive }
    val canEditTarget = goal != null && goal.targetType != Goal.TYPE_CHECKIN

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editInput by rememberSaveable(showEditDialog) {
        mutableStateOf(goal?.targetValue?.toString() ?: "")
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除功课") },
            text = { Text("确定要删除「${pwg.practice.name}」吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showEditDialog) {
        val unit = when {
            goal?.targetType == Goal.TYPE_COURSE -> "课"
            goal?.periodType == Goal.PERIOD_DAILY -> "遍/天"
            else -> "遍"
        }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("修改目标数量") },
            text = {
                OutlinedTextField(
                    value = editInput,
                    onValueChange = { editInput = it.filter { c -> c.isDigit() } },
                    label = { Text("目标（$unit）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val v = editInput.toLongOrNull() ?: return@TextButton
                        if (v > 0) { onEditTarget(v); showEditDialog = false }
                    },
                    enabled = (editInput.toLongOrNull() ?: 0L) > 0
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("取消") }
            }
        )
    }

    val goalLabel = goal?.let {
        when (it.targetType) {
            Goal.TYPE_CHECKIN -> "每日打卡"
            Goal.TYPE_COURSE  -> "课程进度 · ${it.targetValue} 课"
            else -> when (it.periodType) {
                Goal.PERIOD_DAILY -> "每日 ${it.targetValue} 遍"
                else -> "终生累计 · 目标 ${it.targetValue} 遍"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer { translationY = dragOffsetY }
            .background(
                if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
    ) {
        ListItem(
            headlineContent = { Text(pwg.practice.name) },
            supportingContent = goalLabel?.let { label -> { Text(label) } },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "拖动排序",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.pointerInput(pwg.practice.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDrag = { _, amount -> onDrag(amount.y) },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
                )
            },
            trailingContent = {
                Row {
                    if (canEditTarget) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "修改数量",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = if (isDragging)
                    MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
        )
        HorizontalDivider(
            modifier = Modifier
                .padding(start = 56.dp)
                .align(Alignment.BottomStart)
        )
    }
}

private fun moveItem(items: SnapshotStateList<PracticeWithGoals>, from: Int, to: Int) {
    items.add(to, items.removeAt(from))
}
