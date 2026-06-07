package com.meritminder.app.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

private const val COLLECTION = "user_profiles"
private const val FIELD_AVATAR = "avatarBase64"

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val uid get() = auth.currentUser?.uid

    private val _displayName = MutableStateFlow(resolveDisplayName())
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _avatarBase64 = MutableStateFlow<String?>(null)
    val avatarBase64: StateFlow<String?> = _avatarBase64.asStateFlow()

    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    init {
        loadAvatar()
    }

    private fun loadAvatar() {
        val id = uid ?: return
        viewModelScope.launch {
            try {
                val doc = firestore.collection(COLLECTION).document(id).get().await()
                _avatarBase64.value = doc.getString(FIELD_AVATAR)
            } catch (_: Exception) {}
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        val id = uid ?: return
        viewModelScope.launch {
            _uploading.value = true
            _uploadError.value = null
            try {
                val base64 = compressToBase64(context, uri)
                _avatarBase64.value = base64
                firestore.collection(COLLECTION).document(id)
                    .set(mapOf(FIELD_AVATAR to base64))
                    .await()
            } catch (e: Exception) {
                _uploadError.value = e.message ?: "上传失败"
            } finally {
                _uploading.value = false
            }
        }
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim().ifBlank { return }
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(trimmed).build()
                ).await()
                _displayName.value = trimmed
            } catch (_: Exception) {}
        }
    }

    fun clearError() { _uploadError.value = null }

    private fun resolveDisplayName(): String {
        val user = auth.currentUser ?: return ""
        return user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore("@") ?: ""
    }

    private fun compressToBase64(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
        val original = BitmapFactory.decodeStream(stream)
        stream?.close()

        val maxPx = 320
        val scaled = if (original.width > maxPx || original.height > maxPx) {
            val scale = maxPx.toFloat() / maxOf(original.width, original.height)
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            ).also { if (it !== original) original.recycle() }
        } else original

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
        scaled.recycle()

        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
