package com.example.domain.repository

import com.example.domain.model.AppNotification
import com.example.domain.model.AuthSession
import com.example.domain.model.AuthState
import com.example.domain.model.Booking
import com.example.domain.model.BookingStatus
import com.example.domain.model.CrewProfile
import com.example.domain.model.CrewRole
import com.example.domain.model.Message
import com.example.domain.model.RequestEvent
import com.example.domain.model.User
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthenticationRepository {
  val authState: StateFlow<AuthState>
  val currentSession: StateFlow<AuthSession?>
  suspend fun signIn(email: String, password: String): Result<User>
  suspend fun signUp(fullName: String, email: String, password: String): Result<User>
  suspend fun sendPasswordReset(email: String): Result<Unit>
  suspend fun signOut(): Result<Unit>
}

interface UserRepository {
  val authState: StateFlow<AuthState>
  val currentUser: StateFlow<User?>
  suspend fun login(email: String, password: String): Result<User>
  suspend fun register(
    name: String,
    email: String,
    phone: String = "",
    role: UserRole = UserRole.CLIENT,
    companyOrSpecialty: String? = null,
    password: String = ""
  ): Result<User>
  suspend fun logout(): Result<Unit>
  suspend fun updateProfile(
    name: String,
    email: String,
    phone: String,
    bio: String,
    company: String?
  ): Result<User>
  suspend fun getUserById(userId: String): User?
  suspend fun getAllUsers(): List<User> = emptyList()
  suspend fun updateUserRole(userId: String, role: UserRole): Result<User> =
    Result.failure(UnsupportedOperationException("Role assignment not supported"))
  suspend fun restoreSession(): Result<User> =
    Result.failure(IllegalStateException("No saved session"))
  suspend fun sendPasswordReset(email: String): Result<Unit> =
    Result.failure(UnsupportedOperationException("Password reset not supported"))
  suspend fun signInWithGoogle(idToken: String, nonce: String): Result<User> =
    Result.failure(UnsupportedOperationException("Google sign-in not supported"))
}

interface BookingRepository {
  val allBookings: StateFlow<List<Booking>>
  fun getBooking(bookingId: String): Flow<Booking?>
  fun getClientBookings(clientId: String): Flow<List<Booking>>
  fun getCrewBookings(crewId: String): Flow<List<Booking>>
  fun getActiveRequestForClient(clientId: String): Flow<Booking?>
  fun getIncomingRequestsForCrew(crewId: String): Flow<List<Booking>>
  suspend fun createBooking(booking: Booking): Result<Booking>
  suspend fun cancelBooking(bookingId: String): Result<Unit>
  suspend fun acceptBooking(bookingId: String, crewId: String): Result<Booking>
  suspend fun declineBooking(bookingId: String, crewId: String): Result<Unit>
  suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Result<Unit>
}

interface CrewRepository {
  val crewProfiles: StateFlow<List<CrewProfile>>
  fun getCrewProfile(crewId: String): Flow<CrewProfile?>
  fun getCrewProfileByUserId(userId: String): Flow<CrewProfile?>
  suspend fun setAvailability(crewId: String, isAvailable: Boolean): Result<Unit>
  fun getAvailableCrew(role: CrewRole? = null): List<CrewProfile>
  suspend fun updateCrewProfile(profile: CrewProfile): Result<CrewProfile>
}

interface MessageRepository {
  fun getMessages(bookingId: String): Flow<List<Message>>
  suspend fun sendMessage(
    bookingId: String,
    senderId: String,
    senderName: String,
    senderRole: UserRole,
    text: String
  ): Result<Message>
}

interface FavoriteRepository {
  fun getFavoriteCrewIds(clientId: String): Flow<List<String>>
  suspend fun isFavorite(clientId: String, crewUserId: String): Boolean
  suspend fun toggleFavorite(clientId: String, crewUserId: String): Result<Boolean>
}

interface NotificationRepository {
  val requestEvents: SharedFlow<RequestEvent>
  fun getNotifications(userId: String): Flow<List<AppNotification>>
  suspend fun sendNotification(notification: AppNotification)
  suspend fun markAsRead(notificationId: String)
  suspend fun emitRequestEvent(event: RequestEvent)
}
