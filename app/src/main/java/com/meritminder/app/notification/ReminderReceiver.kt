package com.meritminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.Goal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("reminder_id", -1)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pending = buildPendingPractices(context)
                NotificationHelper.sendReminder(context, reminderId, pending)

                // Reschedule for the next day
                if (reminderId != -1) {
                    val reminder = AppDatabase.getInstance(context).reminderDao().getById(reminderId)
                    if (reminder?.enabled == true) ReminderScheduler.schedule(context, reminder)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun buildPendingPractices(context: Context): List<String> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId.isNullOrEmpty()) return emptyList()

        val db = AppDatabase.getInstance(context)
        val today = LocalDate.now()
        val todayStr = today.toString()

        val pwgs = db.practiceDao().getPracticesWithGoals(userId).first()
        val todayRecords = db.dailyRecordDao().getRecordsForDateSync(todayStr)
        val recordMap = todayRecords.associateBy { it.practiceId }

        return pwgs.filter { pwg ->
            val goal = pwg.goals.firstOrNull { it.isActive } ?: return@filter false
            val todayValue = recordMap[pwg.practice.id]?.completedValue ?: 0L

            when {
                goal.periodType == Goal.PERIOD_DAILY ->
                    if (goal.targetType == Goal.TYPE_CHECKIN) todayValue < 1L
                    else todayValue < goal.targetValue

                goal.deadlineDate != null -> {
                    val daysLeft = maxOf(
                        1L,
                        ChronoUnit.DAYS.between(today, LocalDate.parse(goal.deadlineDate)) + 1
                    )
                    val totalDone = db.dailyRecordDao().getTotalCompleted(pwg.practice.id) ?: 0L
                    val remaining = maxOf(0L, goal.targetValue - totalDone)
                    val daily = (remaining + daysLeft - 1) / daysLeft
                    todayValue < daily
                }

                else -> false
            }
        }.map { it.practice.name }
    }
}
