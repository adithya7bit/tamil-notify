package com.tamilnotify.app.repository

import com.tamilnotify.app.model.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton repository holding live captured notification events
 * for UI display and debug observability.
 */
object NotificationStateHolder {
    private const val MAX_HISTORY_SIZE = 50

    private val _notifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val notifications: StateFlow<List<CapturedNotification>> = _notifications.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    fun addNotification(notification: CapturedNotification) {
        _notifications.update { current ->
            // Prepend new notification and cap size
            listOf(notification) + current.take(MAX_HISTORY_SIZE - 1)
        }
    }

    fun updateNotification(key: String, transform: (CapturedNotification) -> CapturedNotification) {
        _notifications.update { current ->
            current.map { if (it.key == key) transform(it) else it }
        }
    }

    fun clear() {
        _notifications.value = emptyList()
    }
}
