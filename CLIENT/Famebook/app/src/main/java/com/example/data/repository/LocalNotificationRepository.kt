package com.example.data.repository

import com.example.data.local.LocalSeedData
import com.example.domain.model.AppNotification
import com.example.domain.model.RequestEvent
import com.example.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map

/**
 * Production-ready local implementation of NotificationRepository.
 * Manages notification streams and in-app request events.
 */
class LocalNotificationRepository : NotificationRepository {
  private val _notifications = MutableStateFlow<List<AppNotification>>(LocalSeedData.initialNotifications)
  private val _requestEvents = MutableSharedFlow<RequestEvent>(replay = 5, extraBufferCapacity = 20)
  override val requestEvents: SharedFlow<RequestEvent> = _requestEvents.asSharedFlow()

  override fun getNotifications(userId: String): Flow<List<AppNotification>> {
    return _notifications.map { list ->
      list.filter { it.recipientUserId == userId || it.recipientUserId == "crew_broadcast" }
        .sortedByDescending { it.timestamp }
    }
  }

  override suspend fun sendNotification(notification: AppNotification) {
    _notifications.value = listOf(notification) + _notifications.value
  }

  override suspend fun markAsRead(notificationId: String) {
    _notifications.value = _notifications.value.map {
      if (it.id == notificationId) it.copy(isRead = true) else it
    }
  }

  override suspend fun emitRequestEvent(event: RequestEvent) {
    _requestEvents.emit(event)
  }
}
