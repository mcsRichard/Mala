package com.meritminder.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {

    const val REQUEST_MORNING = 100
    const val REQUEST_EVENING = 101

    fun schedule(context: Context, requestCode: Int, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = buildPendingIntent(context, requestCode) ?: return

        val triggerAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
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

    fun cancel(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        buildPendingIntent(context, requestCode, PendingIntent.FLAG_NO_CREATE)
            ?.let { alarmManager.cancel(it) }
    }

    fun rescheduleAll(context: Context) {
        val prefs = ReminderPreferences(context)
        if (prefs.morningEnabled) schedule(context, REQUEST_MORNING, prefs.morningHour, prefs.morningMinute)
        if (prefs.eveningEnabled) schedule(context, REQUEST_EVENING, prefs.eveningHour, prefs.eveningMinute)
    }

    private fun buildPendingIntent(
        context: Context,
        requestCode: Int,
        extraFlags: Int = 0
    ): PendingIntent? = PendingIntent.getBroadcast(
        context, requestCode,
        Intent(context, ReminderReceiver::class.java).putExtra("request_code", requestCode),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or extraFlags
    )
}
