package com.meritminder.app.ui.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.PracticeWithGoals
import com.meritminder.app.data.repository.PracticeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class CounterViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val practiceId: Int = savedStateHandle["practiceId"] ?: 0
    private val today = LocalDate.now().toString()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val repo = PracticeRepository(AppDatabase.getInstance(application), userId)

    val practiceWithGoals: StateFlow<PracticeWithGoals?> =
        repo.getPracticeWithGoals(practiceId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _todayCount = MutableStateFlow(0L)
    val todayCount: StateFlow<Long> = _todayCount

    private val _sessionCount = MutableStateFlow(0L)
    val sessionCount: StateFlow<Long> = _sessionCount

    private val _soundEnabled = MutableStateFlow(false)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled

    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            val record = repo.getRecordForDate(practiceId, today)
            _todayCount.value = record?.completedValue ?: 0L
        }
    }

    fun add(amount: Long) {
        _todayCount.value += amount
        _sessionCount.value += amount
        schedulePersist()
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }

    private fun schedulePersist() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(800)
            persist()
        }
    }

    private suspend fun persist() {
        repo.setProgress(practiceId, today, _todayCount.value)
    }

    override fun onCleared() {
        super.onCleared()
        debounceJob?.cancel()
        val snapshot = _todayCount.value
        CoroutineScope(Dispatchers.IO).launch {
            repo.setProgress(practiceId, today, snapshot)
        }
    }
}
