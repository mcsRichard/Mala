package com.meritminder.app.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.remote.Group
import com.meritminder.app.data.remote.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JoinGroupViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repo = GroupRepository()
    val groupId: String = (savedStateHandle.get<String>("groupId") ?: "").uppercase().trim()
    val isLoggedIn: Boolean get() = FirebaseAuth.getInstance().currentUser != null

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _memberCount = MutableStateFlow(0)
    val memberCount: StateFlow<Int> = _memberCount.asStateFlow()

    private val _alreadyMember = MutableStateFlow(false)
    val alreadyMember: StateFlow<Boolean> = _alreadyMember.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _notFound = MutableStateFlow(false)
    val notFound: StateFlow<Boolean> = _notFound.asStateFlow()

    private val _joined = MutableStateFlow(false)
    val joined: StateFlow<Boolean> = _joined.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _targetValue = MutableStateFlow("")
    val targetValue: StateFlow<String> = _targetValue.asStateFlow()

    fun setTargetValue(v: String) { _targetValue.value = v.filter { it.isDigit() } }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _loading.value = true
            try {
                if (!isLoggedIn) return@launch
                val g = repo.getGroup(groupId)
                if (g == null) {
                    _notFound.value = true
                } else {
                    _group.value = g
                    _memberCount.value = repo.getMembers(groupId, g.targetType, g.targetValue).size
                    _alreadyMember.value = repo.isMember(groupId)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "加载失败"
            } finally {
                _loading.value = false
            }
        }
    }

    fun join() {
        viewModelScope.launch {
            try {
                val personal = if (_group.value?.targetType == Group.TYPE_TOTAL) _targetValue.value.toLongOrNull() ?: 0L else 0L
                repo.joinGroup(groupId, personal)
                _joined.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "加入失败"
            }
        }
    }
}
