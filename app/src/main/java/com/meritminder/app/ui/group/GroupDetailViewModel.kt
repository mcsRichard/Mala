package com.meritminder.app.ui.group

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.meritminder.app.data.remote.Group
import com.meritminder.app.data.remote.GroupMember
import com.meritminder.app.data.remote.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupDetailViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repo = GroupRepository()
    val groupId: String = savedStateHandle.get<String>("groupId") ?: ""
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 删除或退出完成后置 true，界面返回上一页 */
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    val isAdmin: Boolean get() = _group.value?.creatorId == uid
    val myMember: GroupMember? get() = _members.value.find { it.userId == uid }

    init {
        refresh()
    }

    fun refresh() {
        if (groupId.isEmpty()) return
        viewModelScope.launch {
            _loading.value = true
            try {
                val g = repo.getGroup(groupId)
                _group.value = g
                if (g != null) _members.value = repo.getMembers(groupId, g.targetType, g.targetValue)
            } catch (e: Exception) {
                _error.value = e.message ?: "加载失败"
            } finally {
                _loading.value = false
            }
        }
    }

    fun checkIn(value: Long) {
        viewModelScope.launch {
            try {
                repo.checkIn(groupId, value)
                val g = _group.value
                _members.value = if (g != null)
                    repo.getMembers(groupId, g.targetType, g.targetValue)
                else
                    repo.getMembers(groupId)
            } catch (e: Exception) {
                _error.value = e.message ?: "打卡失败"
            }
        }
    }

    fun updateMyTarget(targetValue: Long) {
        viewModelScope.launch {
            try {
                repo.updateMemberTarget(groupId, targetValue)
                val g = _group.value
                _members.value = if (g != null)
                    repo.getMembers(groupId, g.targetType, g.targetValue)
                else
                    repo.getMembers(groupId)
            } catch (e: Exception) {
                _error.value = e.message ?: "更新失败"
            }
        }
    }

    fun updateGoal(targetType: String, targetValue: Long) {
        viewModelScope.launch {
            try {
                repo.updateGoal(groupId, targetType, targetValue)
                _group.value = repo.getGroup(groupId)
            } catch (e: Exception) {
                _error.value = e.message ?: "修改失败"
            }
        }
    }

    fun deleteGroup() {
        viewModelScope.launch {
            try {
                repo.deleteGroup(groupId)
                _closed.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "删除失败"
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            try {
                repo.leaveGroup(groupId)
                _closed.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "退出失败"
            }
        }
    }

    fun clearError() { _error.value = null }
}
