package com.meritminder.app.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meritminder.app.data.remote.Group
import com.meritminder.app.data.remote.GroupRepository
import com.meritminder.app.data.remote.GroupStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {

    private val repo = GroupRepository()

    private val _groups = MutableStateFlow<List<GroupStatus>>(emptyList())
    val groups: StateFlow<List<GroupStatus>> = _groups.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 刚创建成功的小组编号，用于弹出分享提示 */
    private val _createdGroupId = MutableStateFlow<String?>(null)
    val createdGroupId: StateFlow<String?> = _createdGroupId.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                _groups.value = repo.getMyGroups()
            } catch (e: Exception) {
                _error.value = e.message ?: "加载失败"
            } finally {
                _loading.value = false
            }
        }
    }

    fun createGroup(name: String, practiceName: String, targetType: String, targetValue: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val id = repo.createGroup(name, practiceName, targetType, targetValue)
                _createdGroupId.value = id
                _groups.value = repo.getMyGroups()
            } catch (e: Exception) {
                _error.value = e.message ?: "创建失败"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearCreated() { _createdGroupId.value = null }
    fun clearError() { _error.value = null }

    companion object {
        fun goalLabel(group: Group): String = when (group.targetType) {
            Group.TYPE_TOTAL -> "总目标 ${group.targetValue} 遍"
            else -> "每日 ${group.targetValue} 遍"
        }
    }
}
