package com.meritminder.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.Reminder
import java.util.Calendar

object ReminderScheduler {

    // Legacy fixed request codes from the old morning/evening system
    private const val LEGACY_MORNING = 100
    private const val LEGACY_EVENING = 101

    fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = buildPendingIntent(context, reminder.id) ?: return

        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        buildPendingIntent(context, reminderId, PendingIntent.FLAG_NO_CREATE)
            ?.let { alarmManager.cancel(it) }
    }

    suspend fun rescheduleAll(context: Context) {
        // Cancel the old hardcoded morning/evening alarms
        cancel(context, LEGACY_MORNING)
        cancel(context, LEGACY_EVENING)

        val db = AppDatabase.getInstance(context)
        db.reminderDao().getAllSync().forEach { r ->
            if (r.enabled) schedule(context, r) else cancel(context, r.id)
        }
    }

    private fun buildPendingIntent(
        context: Context,
        reminderId: Int,
        extraFlags: Int = 0
    ): PendingIntent? = PendingIntent.getBroadcast(
        context, reminderId,
        Intent(context, ReminderReceiver::class.java).putExtra("reminder_id", reminderId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or extraFlags
    )
}
