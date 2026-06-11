package com.meritminder.app.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.DailyRecord
import com.meritminder.app.data.local.entity.Goal
import com.meritminder.app.data.local.entity.PracticeWithGoals
import com.meritminder.app.data.remote.GroupRepository
import com.meritminder.app.data.remote.GroupStatus
import com.meritminder.app.data.repository.PracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DayBarData(
    val date: String,
    val dayLabel: String,
    val completed: Int,
    val total: Int,
    val fraction: Float,
    val isToday: Boolean
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val repo = PracticeRepository(AppDatabase.getInstance(application), userId)
    private val groupRepo = GroupRepository()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _groupStatuses = MutableStateFlow<List<GroupStatus>>(emptyList())
    val groupStatuses: StateFlow<List<GroupStatus>> = _groupStatuses.asStateFlow()
    private val chineseDay = "一二三四五六日"

    init {
        viewModelScope.launch {
            try { _groupStatuses.value = groupRepo.getMyGroups() } catch (_: Exception) {}
        }
    }

    val practicesWithGoals: StateFlow<List<PracticeWithGoals>> =
        repo.getPracticesWithGoals(userId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allTotals: StateFlow<Map<Int, Long>> =
        repo.getAllTotals()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val allRecords = repo.getAllRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalActiveDays: StateFlow<Int> = allRecords.map { records ->
        records.filter { it.completedValue > 0 }.map { it.date }.toSet().size
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val streak: StateFlow<Int> =
        combine(practicesWithGoals, allRecords) { pwgs, records ->
            computeStreak(pwgs, records)
        }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val last7Days: StateFlow<List<DayBarData>> =
        combine(practicesWithGoals, allRecords) { pwgs, records ->
            val today = LocalDate.now()
            val dailyIds = pwgs
                .filter { it.goals.any { g -> g.isActive && g.periodType == Goal.PERIOD_DAILY } }
                .map { it.practice.id }.toSet()
            val byDate = records.groupBy { it.date }

            (6 downTo 0).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val dateStr = date.format(fmt)
                val dayRecords = byDate[dateStr] ?: emptyList()
                val done = dayRecords.count { r -> r.completedValue > 0 && r.practiceId in dailyIds }
                val total = dailyIds.size
                DayBarData(
                    date = dateStr,
                    dayLabel = "周${chineseDay[date.dayOfWeek.value - 1]}",
                    completed = done,
                    total = total,
                    fraction = if (total == 0) 0f else done.toFloat() / total,
                    isToday = daysAgo == 0
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private fun computeStreak(pwgs: List<PracticeWithGoals>, allRecords: List<DailyRecord>): Int {
        val dailyPractices = pwgs.filter { it.goals.any { g -> g.isActive && g.periodType == Goal.PERIOD_DAILY } }
        if (dailyPractices.isEmpty()) return 0
        val byDate = allRecords.groupBy { it.date }
        val today = LocalDate.now()
        var streak = 0
        var day = today.minusDays(1)
        while (streak <= 365) {
            if (!isDayComplete(dailyPractices, byDate[day.format(fmt)] ?: emptyList())) break
            streak++
            day = day.minusDays(1)
        }
        if (isDayComplete(dailyPractices, byDate[today.format(fmt)] ?: emptyList())) streak++
        return streak
    }

    private fun isDayComplete(daily: List<PracticeWithGoals>, records: List<DailyRecord>): Boolean =
        daily.isNotEmpty() && daily.all { pwg ->
            val goal = pwg.goals.firstOrNull { it.isActive } ?: return@all false
            val value = records.find { it.practiceId == pwg.practice.id }?.completedValue ?: 0L
            if (goal.targetType == Goal.TYPE_CHECKIN) value >= 1L else value >= goal.targetValue
        }
}
