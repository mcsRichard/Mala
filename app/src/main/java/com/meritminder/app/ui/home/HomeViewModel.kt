package com.meritminder.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.DailyRecord
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.Practice
import com.meritminder.app.data.local.entity.PracticeWithGoals
import com.meritminder.app.data.remote.GroupRepository
import com.meritminder.app.data.remote.GroupStatus
import com.meritminder.app.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodaySummary(val completed: Int, val total: Int) {
    val progress: Float get() = if (total == 0) 0f else completed.toFloat() / total
    val allDone: Boolean get() = total > 0 && completed == total
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val repository = PracticeRepository(AppDatabase.getInstance(application), userId)
    private val groupRepo = GroupRepository()

    private val _groupStatuses = MutableStateFlow<List<GroupStatus>>(emptyList())
    val groupStatuses: StateFlow<List<GroupStatus>> = _groupStatuses.asStateFlow()
    val today: String = LocalDate.now().toString()

    val practicesWithGoals: StateFlow<List<PracticeWithGoals>> =
        repository.getPracticesWithGoals(userId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val todayRecords: StateFlow<List<DailyRecord>> =
        repository.getTodayRecordsFlow(today)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTotals: StateFlow<Map<Int, Long>> =
        repository.getAllTotals()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val todaySummary: StateFlow<TodaySummary?> =
        combine(practicesWithGoals, todayRecords) { pwgList, records ->
            val dailyOnes = pwgList.filter { pwg ->
                pwg.goals.any { it.isActive && it.periodType == Goal.PERIOD_DAILY }
            }
            if (dailyOnes.isEmpty()) return@combine null
            val completed = dailyOnes.count { pwg ->
                val goal = pwg.goals.firstOrNull { it.isActive } ?: return@count false
                val value = records.find { it.practiceId == pwg.practice.id }?.completedValue ?: 0L
                if (goal.targetType == Goal.TYPE_CHECKIN) value >= 1L else value >= goal.targetValue
            }
            TodaySummary(completed, dailyOnes.size)
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val streak: StateFlow<Int> =
        combine(practicesWithGoals, repository.getAllRecordsFlow()) { pwgList, allRecords ->
            computeStreak(pwgList, allRecords)
        }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    init {
        viewModelScope.launch {
            if (userId.isNotEmpty()) {
                _syncing.value = true
                try {
                    repository.syncWithFirestore()
                } catch (_: Exception) {}
                _syncing.value = false
            }
        }
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            try { _groupStatuses.value = groupRepo.getMyGroups() } catch (_: Exception) {}
        }
    }

    fun checkInGroup(groupId: String, value: Long) {
        viewModelScope.launch {
            try {
                groupRepo.checkIn(groupId, value)
                _groupStatuses.value = groupRepo.getMyGroups()
            } catch (_: Exception) {}
        }
    }

    fun logProgress(practiceId: Int, additionalValue: Long) {
        viewModelScope.launch { repository.logProgress(practiceId, today, additionalValue) }
    }

    fun toggleCheckin(practiceId: Int) {
        viewModelScope.launch {
            val existing = repository.getRecordForDate(practiceId, today)
            val newValue = if ((existing?.completedValue ?: 0L) >= 1L) 0L else 1L
            repository.setProgress(practiceId, today, newValue)
        }
    }

    fun deletePractice(practice: Practice) {
        viewModelScope.launch { repository.deletePractice(practice) }
    }

    fun updateSortOrder(reorderedPractices: List<Practice>) {
        viewModelScope.launch { repository.updateSortOrders(reorderedPractices) }
    }

    fun updateGoalTarget(practiceId: Int, newTarget: Long) {
        viewModelScope.launch { repository.updateGoalTarget(practiceId, newTarget) }
    }

    private fun computeStreak(pwgList: List<PracticeWithGoals>, allRecords: List<DailyRecord>): Int {
        val dailyPractices = pwgList.filter { pwg ->
            pwg.goals.any { it.isActive && it.periodType == Goal.PERIOD_DAILY }
        }
        if (dailyPractices.isEmpty()) return 0

        val recordsByDate = allRecords.groupBy { it.date }
        val todayDate = LocalDate.now()

        // Count consecutive complete days going back from yesterday
        var streak = 0
        var checkDate = todayDate.minusDays(1)
        while (true) {
            val dayRecords = recordsByDate[checkDate.toString()] ?: emptyList()
            if (isDayComplete(dailyPractices, dayRecords)) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                break
            }
        }

        // If today is also complete, include it
        val todayRecords = recordsByDate[todayDate.toString()] ?: emptyList()
        if (isDayComplete(dailyPractices, todayRecords)) streak++

        return streak
    }

    private fun isDayComplete(dailyPractices: List<PracticeWithGoals>, dayRecords: List<DailyRecord>): Boolean =
        dailyPractices.isNotEmpty() && dailyPractices.all { pwg ->
            val goal = pwg.goals.firstOrNull { it.isActive } ?: return@all false
            val value = dayRecords.find { it.practiceId == pwg.practice.id }?.completedValue ?: 0L
            if (goal.targetType == Goal.TYPE_CHECKIN) value >= 1L else value >= goal.targetValue
        }
}
