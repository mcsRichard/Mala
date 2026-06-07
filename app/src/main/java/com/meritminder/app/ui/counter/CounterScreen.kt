package com.meritminder.app.ui.counter

import android.app.Activity
import android.content.res.Configuration
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meritminder.app.data.local.entity.Goal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(
    onNavigateBack: () -> Unit,
    vm: CounterViewModel = viewModel()
) {
    val pwg by vm.practiceWithGoals.collectAsState()
    val todayCount by vm.todayCount.collectAsState()
    val sessionCount by vm.sessionCount.collectAsState()
    val soundEnabled by vm.soundEnabled.collectAsState()
    val goal = pwg?.goals?.firstOrNull { it.isActive }
    val practiceName = pwg?.practice?.name ?: ""
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    val view = LocalView.current
    val toneGen = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 30) }

    // Keep screen on while counter is open
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            toneGen.release()
        }
    }

    val onTap: (Long) -> Unit = { amount ->
        vm.add(amount)
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        if (soundEnabled) toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 60)
    }

    if (isLandscape) {
        LandscapeCounter(
            practiceName = practiceName,
            todayCount = todayCount,
            sessionCount = sessionCount,
            soundEnabled = soundEnabled,
            onTap = { onTap(1L) },
            onQuickAdd = { onTap(it) },
            onToggleSound = { vm.toggleSound() },
            onNavigateBack = onNavigateBack
        )
    } else {
        PortraitCounter(
            practiceName = practiceName,
            goal = goal,
            todayCount = todayCount,
            sessionCount = sessionCount,
            soundEnabled = soundEnabled,
            onTap = { onTap(1L) },
            onQuickAdd = { onTap(it) },
            onToggleSound = { vm.toggleSound() },
            onNavigateBack = onNavigateBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitCounter(
    practiceName: String,
    goal: Goal?,
    todayCount: Long,
    sessionCount: Long,
    soundEnabled: Boolean,
    onTap: () -> Unit,
    onQuickAdd: (Long) -> Unit,
    onToggleSound: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(practiceName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSound) {
                        Icon(
                            if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = if (soundEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Progress info
            if (goal != null) {
                ProgressSection(goal = goal, todayCount = todayCount)
            }

            Spacer(Modifier.weight(1f))

            // Main bead button
            BeadButton(count = todayCount, size = 220, onClick = onTap)

            Spacer(Modifier.height(16.dp))

            // Session count
            Text(
                text = "本次 +$sessionCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.weight(1f))

            // Quick add buttons
            QuickAddRow(onQuickAdd = onQuickAdd)

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LandscapeCounter(
    practiceName: String,
    todayCount: Long,
    sessionCount: Long,
    soundEnabled: Boolean,
    onTap: () -> Unit,
    onQuickAdd: (Long) -> Unit,
    onToggleSound: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTap() }
    ) {
        // Count display in center
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$todayCount",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "本次 +$sessionCount",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Top controls
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
            }
            Text(
                text = practiceName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        IconButton(
            onClick = onToggleSound,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(
                if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (soundEnabled) 1f else 0.5f)
            )
        }

        // Quick add at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(7L, 21L, 108L).forEach { n ->
                QuickAddChip(
                    label = "+$n",
                    onClick = { onQuickAdd(n) },
                    textColor = Color.White,
                    borderColor = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ProgressSection(goal: Goal, todayCount: Long) {
    val (current, target, label) = when {
        goal.targetType == Goal.TYPE_CHECKIN -> Triple(todayCount, 1L, "打卡")
        goal.periodType == Goal.PERIOD_DAILY -> Triple(todayCount, goal.targetValue, "今日")
        goal.targetType == Goal.TYPE_COURSE -> Triple(todayCount, goal.targetValue, "课程")
        else -> Triple(todayCount, 0L, "累计")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (target > 0) {
                Text(
                    text = "$current / $target",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "累计 $current 遍",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (target > 0) {
            Spacer(Modifier.height(8.dp))
            val progress by animateFloatAsState(
                targetValue = (current.toFloat() / target).coerceIn(0f, 1f),
                animationSpec = tween(300), label = "progress"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun BeadButton(count: Long, size: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$count",
            fontSize = (size / 4).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun QuickAddRow(onQuickAdd: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(7L, 21L, 108L).forEach { n ->
            FilledTonalButton(onClick = { onQuickAdd(n) }) {
                Text("+$n")
            }
        }
    }
}

@Composable
private fun QuickAddChip(
    label: String,
    onClick: () -> Unit,
    textColor: Color,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = textColor, style = MaterialTheme.typography.labelLarge)
    }
}
