package com.meritminder.app.ui.group

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meritminder.app.data.remote.Group
import com.meritminder.app.data.remote.GroupStatus

@Composable
fun GroupScreen(
    contentPadding: PaddingValues,
    onOpenGroup: (String) -> Unit,
    onJoinByCode: (String) -> Unit,
    viewModel: GroupViewModel = viewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val createdGroupId by viewModel.createdGroupId.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    // 创建成功后直接进入详情页（里面可以分享）
    LaunchedEffect(createdGroupId) {
        createdGroupId?.let {
            viewModel.clearCreated()
            onOpenGroup(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("创建小组")
            }
            OutlinedButton(
                onClick = { showJoinDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("加入小组")
            }
            IconButton(onClick = { viewModel.refresh() }, enabled = !loading) {
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            loading && groups.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            groups.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text("还没有加入小组", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "创建小组邀请道友，或输入编号加入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(groups, key = { it.group.id }) { status ->
                    GroupCard(status = status, onClick = { onOpenGroup(status.group.id) })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateGroupDialog(
            onCreate = { name, practice, type, target ->
                viewModel.createGroup(name, practice, type, target)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showJoinDialog) {
        JoinByCodeDialog(
            onJoin = { code ->
                showJoinDialog = false
                onJoinByCode(code)
            },
            onDismiss = { showJoinDialog = false }
        )
    }
}

@Composable
private fun GroupCard(status: GroupStatus, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = status.group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (status.myDoneToday) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${status.group.practiceName} · ${GroupViewModel.goalLabel(status.group)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row {
                Text(
                    text = "今日 ${status.todayDoneCount}/${status.memberCount} 人已打卡",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "我的累计 ${status.myTotal} 遍",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    onCreate: (name: String, practiceName: String, targetType: String, targetValue: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var practiceName by rememberSaveable { mutableStateOf("") }
    var targetType by rememberSaveable { mutableStateOf(Group.TYPE_CHECKIN) }
    var targetValue by rememberSaveable { mutableStateOf("") }

    val isValid = name.isNotBlank() && practiceName.isNotBlank() &&
        (targetValue.toLongOrNull() ?: 0L) > 0L

    val quantityLabel = if (targetType == Group.TYPE_CHECKIN) "每人每日目标数量（遍）*"
                        else "每人总目标数量（遍）*"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建小组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("小组名称 *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = practiceName,
                    onValueChange = { practiceName = it },
                    label = { Text("共修功课（如：百字明）*") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Column {
                    Text("功课目标（所有组员相同）", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = targetType == Group.TYPE_CHECKIN,
                            onClick = { targetType = Group.TYPE_CHECKIN; targetValue = "" },
                            label = { Text("当日完成") }
                        )
                        FilterChip(
                            selected = targetType == Group.TYPE_TOTAL,
                            onClick = { targetType = Group.TYPE_TOTAL; targetValue = "" },
                            label = { Text("总目标") }
                        )
                    }
                }
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it.filter { c -> c.isDigit() } },
                    label = { Text(quantityLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(name.trim(), practiceName.trim(), targetType, targetValue.toLongOrNull() ?: 0L)
                },
                enabled = isValid
            ) { Text("创建") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun JoinByCodeDialog(
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入小组") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "输入小组编号（6 位字母数字）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(6) },
                    label = { Text("小组编号") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onJoin(code.trim()) },
                enabled = code.trim().length == 6
            ) { Text("下一步") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
