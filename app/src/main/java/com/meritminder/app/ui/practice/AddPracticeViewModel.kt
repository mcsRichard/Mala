package com.meritminder.app.ui.practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.Practice
import com.meritminder.app.data.local.entity.PracticeWithGoals
import com.meritminder.app.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AddPracticeViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val repository = PracticeRepository(AppDatabase.getInstance(application), userId)

    val initialName: String = savedStateHandle.get<String>("name") ?: ""
    val editPracticeId: Int = savedStateHandle.get<Int>("practiceId") ?: -1
    val isEditMode: Boolean = editPracticeId != -1

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _loadedData = MutableStateFlow<PracticeWithGoals?>(null)
    val loadedData: StateFlow<PracticeWithGoals?> = _loadedData.asStateFlow()

    init {
        if (isEditMode) {
            viewModelScope.launch {
                _loadedData.value = repository.getPracticeWithGoals(editPracticeId).first()
            }
        }
    }

    fun addPractice(
        name: String,
        practiceType: String,
        goalPreset: String,
        targetValue: Long,
        deadlineDate: String? = null
    ) {
        if (name.isBlank()) return
        if (goalPreset != PRESET_CHECKIN && targetValue <= 0) return
        if (goalPreset == PRESET_DEADLINE && deadlineDate == null) return
        viewModelScope.launch {
            val (targetType, periodType, actualTarget) = resolveGoalParams(goalPreset, targetValue)
            val practice = Practice(userId = userId, name = name.trim(), type = practiceType)
            val goal = Goal(
                practiceId = 0,
                targetType = targetType,
                targetValue = actualTarget,
                periodType = periodType,
                deadlineDate = if (goalPreset == PRESET_DEADLINE) deadlineDate else null
            )
            repository.addPractice(practice, goal)
            _saved.value = true
        }
    }

    fun updatePractice(
        name: String,
        practiceType: String,
        goalPreset: String,
        targetValue: Long,
        deadlineDate: String? = null
    ) {
        if (name.isBlank()) return
        if (goalPreset != PRESET_CHECKIN && targetValue <= 0) return
        if (goalPreset == PRESET_DEADLINE && deadlineDate == null) return
        val existing = _loadedData.value ?: return
        val activeGoal = existing.goals.firstOrNull { it.isActive } ?: return
        viewModelScope.launch {
            val (targetType, periodType, actualTarget) = resolveGoalParams(goalPreset, targetValue)
            val updatedPractice = existing.practice.copy(name = name.trim(), type = practiceType)
            val updatedGoal = activeGoal.copy(
                targetType = targetType,
                targetValue = actualTarget,
                periodType = periodType,
                deadlineDate = if (goalPreset == PRESET_DEADLINE) deadlineDate else null
            )
            repository.updatePractice(updatedPractice, updatedGoal)
            _saved.value = true
        }
    }

    fun resetSaved() { _saved.value = false }

    private fun resolveGoalParams(goalPreset: String, targetValue: Long): Triple<String, String, Long> =
        when (goalPreset) {
            PRESET_DAILY_COUNT -> Triple(Goal.TYPE_COUNT, Goal.PERIOD_DAILY, targetValue)
            PRESET_CHECKIN     -> Triple(Goal.TYPE_CHECKIN, Goal.PERIOD_DAILY, 1L)
            PRESET_LIFETIME    -> Triple(Goal.TYPE_COUNT, Goal.PERIOD_LONG_TERM, targetValue)
            PRESET_COURSE      -> Triple(Goal.TYPE_COURSE, Goal.PERIOD_LONG_TERM, targetValue)
            PRESET_DEADLINE    -> Triple(Goal.TYPE_COUNT, Goal.PERIOD_LONG_TERM, targetValue)
            else               -> Triple(Goal.TYPE_COUNT, Goal.PERIOD_DAILY, targetValue)
        }

    companion object {
        const val PRESET_DAILY_COUNT = "DAILY_COUNT"
        const val PRESET_CHECKIN = "CHECKIN"
        const val PRESET_LIFETIME = "LIFETIME"
        const val PRESET_COURSE = "COURSE"
        const val PRESET_DEADLINE = "DEADLINE"

        fun goalToPreset(goal: Goal): String = when {
            goal.targetType == Goal.TYPE_CHECKIN                -> PRESET_CHECKIN
            goal.targetType == Goal.TYPE_COURSE                 -> PRESET_COURSE
            goal.periodType == Goal.PERIOD_DAILY                -> PRESET_DAILY_COUNT
            goal.deadlineDate != null                           -> PRESET_DEADLINE
            else                                                -> PRESET_LIFETIME
        }
    }
}
