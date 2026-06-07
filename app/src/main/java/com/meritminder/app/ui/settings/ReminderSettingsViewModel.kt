package com.meritminder.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.meritminder.app.notification.ReminderPreferences
import com.meritminder.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = application
    private val prefs = ReminderPreferences(ctx)

    private val _morningEnabled = MutableStateFlow(prefs.morningEnabled)
    val morningEnabled: StateFlow<Boolean> = _morningEnabled.asStateFlow()

    private val _morningHour = MutableStateFlow(prefs.morningHour)
    val morningHour: StateFlow<Int> = _morningHour.asStateFlow()

    private val _morningMinute = MutableStateFlow(prefs.morningMinute)
    val morningMinute: StateFlow<Int> = _morningMinute.asStateFlow()

    private val _eveningEnabled = MutableStateFlow(prefs.eveningEnabled)
    val eveningEnabled: StateFlow<Boolean> = _eveningEnabled.asStateFlow()

    private val _eveningHour = MutableStateFlow(prefs.eveningHour)
    val eveningHour: StateFlow<Int> = _eveningHour.asStateFlow()

    private val _eveningMinute = MutableStateFlow(prefs.eveningMinute)
    val eveningMinute: StateFlow<Int> = _eveningMinute.asStateFlow()

    fun setMorningEnabled(enabled: Boolean) {
        prefs.morningEnabled = enabled
        _morningEnabled.value = enabled
        if (enabled) ReminderScheduler.schedule(ctx, ReminderScheduler.REQUEST_MORNING, prefs.morningHour, prefs.morningMinute)
        else ReminderScheduler.cancel(ctx, ReminderScheduler.REQUEST_MORNING)
    }

    fun setMorningTime(hour: Int, minute: Int) {
        prefs.morningHour = hour
        prefs.morningMinute = minute
        _morningHour.value = hour
        _morningMinute.value = minute
        if (prefs.morningEnabled) ReminderScheduler.schedule(ctx, ReminderScheduler.REQUEST_MORNING, hour, minute)
    }

    fun setEveningEnabled(enabled: Boolean) {
        prefs.eveningEnabled = enabled
        _eveningEnabled.value = enabled
        if (enabled) ReminderScheduler.schedule(ctx, ReminderScheduler.REQUEST_EVENING, prefs.eveningHour, prefs.eveningMinute)
        else ReminderScheduler.cancel(ctx, ReminderScheduler.REQUEST_EVENING)
    }

    fun setEveningTime(hour: Int, minute: Int) {
        prefs.eveningHour = hour
        prefs.eveningMinute = minute
        _eveningHour.value = hour
        _eveningMinute.value = minute
        if (prefs.eveningEnabled) ReminderScheduler.schedule(ctx, ReminderScheduler.REQUEST_EVENING, hour, minute)
    }
}
