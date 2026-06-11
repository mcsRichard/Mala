package com.meritminder.app.ui.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinGroupScreen(
    onNavigateBack: () -> Unit,
    onJoined: () -> Unit,
    viewModel: JoinGroupViewModel = viewModel()
) {
    val group by viewModel.group.collectAsState()
    val memberCount by viewModel.memberCount.collectAsState()
    val alreadyMember by viewModel.alreadyMember.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val notFound by viewModel.notFound.collectAsState()
    val joined by viewModel.joined.collectAsState()
    val error by viewModel.error.collectAsState()
    val targetValue by viewModel.targetValue.collectAsState()

    LaunchedEffect(joined) { if (joined) onJoined() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("加入小组") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                !viewModel.isLoggedIn -> Message("请先登录 Mala 账号，再点击邀请链接加入小组")

                loading -> CircularProgressIndicator()

                notFound -> Message("小组「${viewModel.groupId}」不存在或已解散")

                error != null -> Message("加载失败：$error")

                group != null -> {
                    val g = group!!
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            g.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                InfoLine("共修功课", g.practiceName)
                                InfoLine("功课目标", GroupViewModel.goalLabel(g))
                                InfoLine("组员人数", "$memberCount 人")
                                InfoLine("创建者", g.creatorName)
                                InfoLine("小组编号", g.id)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (alreadyMember) {
                            Text(
                                "你已经是该小组成员",
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedButton(
                                onClick = onJoined,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) { Text("前往小组") }
                        } else {
                            if (viewModel.needsPersonalTarget) {
                                OutlinedTextField(
                                    value = targetValue,
                                    onValueChange = { viewModel.setTargetValue(it) },
                                    label = { Text("我的总目标数量（遍）*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            Button(
                                onClick = { viewModel.join() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = !viewModel.needsPersonalTarget ||
                                    (targetValue.toLongOrNull() ?: 0L) > 0L
                            ) { Text("确认加入") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    androidx.compose.foundation.layout.Row {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
