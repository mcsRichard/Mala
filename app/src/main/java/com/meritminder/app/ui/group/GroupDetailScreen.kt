package com.meritminder.app.ui.group

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meritminder.app.data.remote.Group
import com.meritminder.app.data.remote.GroupMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: GroupDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val group by viewModel.group.collectAsState()
    val members by viewModel.members.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val closed by viewModel.closed.collectAsState()

    var showCheckInDialog by remember { mutableStateOf(false) }
    var showEditGoalDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(closed) { if (closed) onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(group?.name ?: "小组") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }, enabled = !loading) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                    IconButton(onClick = {
                        group?.let { shareGroup(context, it) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                    if (viewModel.isAdmin && group?.targetType == Group.TYPE_CHECKIN) {
                        IconButton(onClick = { showEditGoalDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "修改目标")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "解散小组",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else if (group != null) {
                        IconButton(onClick = { showLeaveConfirm = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "退出小组",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val g = group
        when {
            loading && g == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            g == null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("小组不存在或已解散", color = MaterialTheme.colorScheme.onSurfaceVariant) }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                item { GroupInfoCard(g, members.size) }

                item {
                    val me = viewModel.myMember
                    CheckInCard(
                        group = g,
                        me = me,
                        onCheckIn = { showCheckInDialog = true }
                    )
                }

                item {
                    Text(
                        "组员（${members.size}）",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(members, key = { it.userId }) { member ->
                    MemberRow(member = member, group = g)
                    HorizontalDivider()
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showCheckInDialog) {
        CheckInAmountDialog(
            onConfirm = { amount ->
                viewModel.checkIn(amount)
                showCheckInDialog = false
            },
            onDismiss = { showCheckInDialog = false }
        )
    }

    if (showEditGoalDialog && group != null) {
        EditGoalDialog(
            group = group!!,
            onConfirm = { type, value ->
                viewModel.updateGoal(type, value)
                showEditGoalDialog = false
            },
            onDismiss = { showEditGoalDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("解散小组") },
            text = { Text("确定要解散「${group?.name}」吗？所有组员的打卡记录将被删除，此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.deleteGroup() }) {
                    Text("解散", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("退出小组") },
            text = { Text("确定要退出「${group?.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { showLeaveConfirm = false; viewModel.leaveGroup() }) {
                    Text("退出", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("取消") }
            }
        )
    }
}

private fun shareGroup(context: android.content.Context, group: Group) {
    val link = "https://mcsrichard.github.io/Mala/join.html?code=${group.id}"
    val text = "邀请你加入共修小组「${group.name}」\n" +
        "功课：${group.practiceName}（${GroupViewModel.goalLabel(group)}）\n\n" +
        "点击链接加入：$link\n" +
        "（微信用户请在浏览器中打开，或手动输入编号：${group.id}）"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "分享小组"))
}

@Composable
private fun GroupInfoCard(group: Group, memberCount: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text(
                    text = group.practiceName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = group.id,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${GroupViewModel.goalLabel(group)} · $memberCount 名组员 · 创建者 ${group.creatorName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CheckInCard(group: Group, me: GroupMember?, onCheckIn: () -> Unit) {
    val doneToday = me?.doneToday == true
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (group.targetType == Group.TYPE_TOTAL) {
                val total = me?.total ?: 0L
                val myTarget = me?.targetValue ?: 0L
                val fraction = if (myTarget > 0)
                    (total.toFloat() / myTarget).coerceIn(0f, 1f) else 0f
                Text(
                    "我的进度：$total / $myTarget",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth()) {
                    Text(if (doneToday) "继续记录" else "今日打卡")
                }
            } else {
                val todayValue = me?.todayValue ?: 0L
                val fraction = if (group.targetValue > 0)
                    (todayValue.toFloat() / group.targetValue).coerceIn(0f, 1f) else 0f
                Text(
                    "今日进度：$todayValue / ${group.targetValue}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth()) {
                    Text(if (doneToday) "继续记录" else "记录")
                }
            }
        }
    }
}

@Composable
private fun MemberRow(member: GroupMember, group: Group) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (member.doneToday) Icons.Default.CheckCircle
                          else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (member.doneToday) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(member.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (member.doneToday) "今日已打卡" else "今日未打卡",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (group.targetType == Group.TYPE_TOTAL && member.targetValue > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${member.total} / ${member.targetValue}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "累计",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${member.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "累计",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CheckInAmountDialog(
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录今日完成数量") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() } },
                label = { Text("完成数量（遍）") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount.toLongOrNull() ?: 0L) },
                enabled = (amount.toLongOrNull() ?: 0L) > 0L
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun EditGoalDialog(
    group: Group,
    onConfirm: (String, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var targetValue by rememberSaveable { mutableStateOf(
        if (group.targetValue > 0) group.targetValue.toString() else ""
    ) }
    val isValid = (targetValue.toLongOrNull() ?: 0L) > 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改每日目标") },
        text = {
            OutlinedTextField(
                value = targetValue,
                onValueChange = { targetValue = it.filter { c -> c.isDigit() } },
                label = { Text("每人每日目标数量（遍）") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(Group.TYPE_CHECKIN, targetValue.toLongOrNull() ?: 0L) },
                enabled = isValid
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
