package com.meritminder.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meritminder.app.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra("request_code", ReminderScheduler.REQUEST_MORNING)
        val prefs = ReminderPreferences(context)

        val title = context.getString(R.string.notif_title)
        val quotes = context.resources.getStringArray(R.array.buddhist_quotes)
        val body = quotes.random()

        NotificationHelper.send(
            context,
            if (requestCode == ReminderScheduler.REQUEST_MORNING) NotificationHelper.ID_MORNING
            else NotificationHelper.ID_EVENING,
            title, body
        )

        // Re-schedule for next day
        when (requestCode) {
            ReminderScheduler.REQUEST_MORNING ->
                if (prefs.morningEnabled)
                    ReminderScheduler.schedule(context, requestCode, prefs.morningHour, prefs.morningMinute)
            ReminderScheduler.REQUEST_EVENING ->
                if (prefs.eveningEnabled)
                    ReminderScheduler.schedule(context, requestCode, prefs.eveningHour, prefs.eveningMinute)
        }
    }
}
