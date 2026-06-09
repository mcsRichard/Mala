package com.meritminder.app.ui.transmission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.local.AppDatabase
import com.meritminder.app.data.local.entity.Transmission
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransmissionViewModel(application: Application) : AndroidViewModel(application) {

    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val dao = AppDatabase.getInstance(application).transmissionDao()

    val transmissions = dao.getAll(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(name: String, teacher: String, date: String, place: String, notes: String) {
        if (name.isBlank() || teacher.isBlank() || date.isBlank() || place.isBlank()) return
        viewModelScope.launch {
            dao.insert(
                Transmission(
                    userId = userId,
                    name = name.trim(),
                    teacher = teacher.trim(),
                    date = date,
                    place = place.trim(),
                    notes = notes.trim()
                )
            )
        }
    }

    fun update(t: Transmission, name: String, teacher: String, date: String, place: String, notes: String) {
        if (name.isBlank() || teacher.isBlank() || date.isBlank() || place.isBlank()) return
        viewModelScope.launch {
            dao.update(t.copy(name = name.trim(), teacher = teacher.trim(), date = date, place = place.trim(), notes = notes.trim()))
        }
    }

    fun delete(t: Transmission) {
        viewModelScope.launch { dao.delete(t) }
    }
}
