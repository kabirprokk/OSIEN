package com.example.data.repository

import com.example.data.local.LocalSeedData
import com.example.domain.model.Message
import com.example.domain.model.UserRole
import com.example.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Production-ready local implementation of MessageRepository.
 * Stores conversation messages between clients and confirmed crew members in memory.
 */
class LocalMessageRepository : MessageRepository {
  private val _messages = MutableStateFlow<List<Message>>(LocalSeedData.initialMessages)

  override fun getMessages(bookingId: String): Flow<List<Message>> {
    return _messages.map { list ->
      list.filter { it.bookingId == bookingId }
        .sortedBy { it.timestamp }
    }
  }

  override suspend fun sendMessage(
    bookingId: String,
    senderId: String,
    senderName: String,
    senderRole: UserRole,
    text: String
  ): Result<Message> {
    if (text.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty."))

    val msg = Message(
      id = "msg_${UUID.randomUUID().toString().take(8)}",
      bookingId = bookingId,
      senderId = senderId,
      senderName = senderName,
      senderRole = senderRole,
      text = text.trim(),
      timestamp = System.currentTimeMillis()
    )

    _messages.value = _messages.value + msg
    return Result.success(msg)
  }
}
