package com.meritminder.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meritminder.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loginState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val loginState: StateFlow<AuthUiState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val registerState: StateFlow<AuthUiState> = _registerState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthUiState.Loading
            _loginState.value = repository.login(email.trim(), password).fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.localizedMessage ?: "Login failed") }
            )
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _registerState.value = AuthUiState.Loading
            _registerState.value = repository.register(email.trim(), password).fold(
                onSuccess = { AuthUiState.Success },
                onFailure = { AuthUiState.Error(it.localizedMessage ?: "Registration failed") }
            )
        }
    }

    fun logout() = repository.logout()

    fun resetLoginState() { _loginState.value = AuthUiState.Idle }
    fun resetRegisterState() { _registerState.value = AuthUiState.Idle }
}
