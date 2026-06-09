package com.meritminder.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.Reminder
import com.meritminder.app.notification.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val ctx = application
    private val dao = AppDatabase.getInstance(application).reminderDao()

    val reminders = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(hour: Int, minute: Int) {
        viewModelScope.launch {
            val id = dao.insert(Reminder(hour = hour, minute = minute, enabled = true)).toInt()
            ReminderScheduler.schedule(ctx, Reminder(id = id, hour = hour, minute = minute))
        }
    }

    fun updateTime(reminder: Reminder, hour: Int, minute: Int) {
        viewModelScope.launch {
            val updated = reminder.copy(hour = hour, minute = minute)
            dao.update(updated)
            if (updated.enabled) ReminderScheduler.schedule(ctx, updated)
        }
    }

    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        viewModelScope.launch {
            val updated = reminder.copy(enabled = enabled)
            dao.update(updated)
            if (enabled) ReminderScheduler.schedule(ctx, updated)
            else ReminderScheduler.cancel(ctx, reminder.id)
        }
    }

    fun delete(reminder: Reminder) {
        viewModelScope.launch {
            ReminderScheduler.cancel(ctx, reminder.id)
            dao.delete(reminder)
        }
    }
}
