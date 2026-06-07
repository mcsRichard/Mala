package com.meritminder.app.notification

import android.content.Context

class ReminderPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("reminders", Context.MODE_PRIVATE)

    var morningEnabled: Boolean
        get() = prefs.getBoolean("morning_enabled", false)
        set(v) = prefs.edit().putBoolean("morning_enabled", v).apply()

    var morningHour: Int
        get() = prefs.getInt("morning_hour", 8)
        set(v) = prefs.edit().putInt("morning_hour", v).apply()

    var morningMinute: Int
        get() = prefs.getInt("morning_minute", 0)
        set(v) = prefs.edit().putInt("morning_minute", v).apply()

    var eveningEnabled: Boolean
        get() = prefs.getBoolean("evening_enabled", false)
        set(v) = prefs.edit().putBoolean("evening_enabled", v).apply()

    var eveningHour: Int
        get() = prefs.getInt("evening_hour", 20)
        set(v) = prefs.edit().putInt("evening_hour", v).apply()

    var eveningMinute: Int
        get() = prefs.getInt("evening_minute", 0)
        set(v) = prefs.edit().putInt("evening_minute", v).apply()
}
