package com.example.data.repository

import com.example.data.local.LocalSeedData
import com.example.domain.model.AuthState
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Local implementation of UserRepository.
 * Manages user accounts and profiles.
 * Designed for future transition to Cloud/Room persistence.
 */
class LocalUserRepository : UserRepository {

  private val usersList = mutableListOf<User>().apply {
    addAll(LocalSeedData.initialUsers)
  }
  private val passwords = mutableMapOf<String, String>()

  // Active authenticated user
  private val _currentUser = MutableStateFlow<User?>(null)
  override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
  override val authState: StateFlow<AuthState> = _authState.asStateFlow()

  fun setCurrentUser(user: User?) {
    _currentUser.value = user
    if (user != null) {
      _authState.value = AuthState.Authenticated(user)
    } else {
      _authState.value = AuthState.Unauthenticated
    }
  }

  override suspend fun getAllUsers(): List<User> {
    return usersList.toList()
  }

  override suspend fun login(email: String, password: String): Result<User> {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank()) {
      _authState.value = AuthState.AuthenticationError("Email is required.")
      return Result.failure(IllegalArgumentException("Email is required."))
    }
    if (password.length < 4) {
      _authState.value = AuthState.AuthenticationError("Password must be at least 4 characters.")
      return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
    }

    _authState.value = AuthState.Authenticating

    val matchedUser = usersList.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
      ?: return Result.failure(IllegalArgumentException("No account found for this email."))
    if (passwords[matchedUser.email.lowercase()] != password) {
      _authState.value = AuthState.AuthenticationError("Incorrect email or password.")
      return Result.failure(IllegalArgumentException("Incorrect email or password."))
    }

    _currentUser.value = matchedUser
    _authState.value = AuthState.Authenticated(matchedUser)
    return Result.success(matchedUser)
  }

  override suspend fun register(
    name: String,
    email: String,
    phone: String,
    role: UserRole,
    companyOrSpecialty: String?,
    password: String
  ): Result<User> {
    if (name.isBlank() || email.isBlank() || !email.contains("@")) {
      val err = "Enter your full name and a valid email address."
      _authState.value = AuthState.AuthenticationError(err)
      return Result.failure(IllegalArgumentException(err))
    }

    if (passwords.containsKey(email.trim().lowercase())) {
      val err = "An account with this email already exists. Please sign in."
      _authState.value = AuthState.AuthenticationError(err)
      return Result.failure(IllegalArgumentException(err))
    }

    _authState.value = AuthState.Authenticating

    val newUser = User(
      id = "usr_${UUID.randomUUID().toString().take(8)}",
      name = name.trim(),
      email = email.trim(),
      phone = phone.trim().ifBlank { "+91 98000 00000" },
      role = role,
      companyName = companyOrSpecialty?.trim()?.ifBlank { null },
      bio = if (role == UserRole.CREW) "Professional FameBros verified crew." else "Creative client on FameBook."
    )

    usersList.removeAll { it.email.equals(email.trim(), ignoreCase = true) }
    usersList.add(newUser)
    passwords[newUser.email.lowercase()] = password

    _currentUser.value = newUser
    _authState.value = AuthState.Authenticated(newUser)
    return Result.success(newUser)
  }

  override suspend fun logout(): Result<Unit> {
    _currentUser.value = null
    _authState.value = AuthState.Unauthenticated
    return Result.success(Unit)
  }

  override suspend fun updateProfile(
    name: String,
    email: String,
    phone: String,
    bio: String,
    company: String?
  ): Result<User> {
    val current = _currentUser.value
      ?: return Result.failure(IllegalStateException("No active user session."))

    val updated = current.copy(
      name = name.trim().ifBlank { current.name },
      email = email.trim().ifBlank { current.email },
      phone = phone.trim().ifBlank { current.phone },
      bio = bio.trim(),
      companyName = company?.trim()?.ifBlank { current.companyName }
    )

    val index = usersList.indexOfFirst { it.id == current.id }
    if (index != -1) {
      usersList[index] = updated
    } else {
      usersList.add(updated)
    }

    _currentUser.value = updated
    _authState.value = AuthState.Authenticated(updated)
    return Result.success(updated)
  }

  override suspend fun getUserById(userId: String): User? {
    return usersList.firstOrNull { it.id == userId }
  }

  override suspend fun updateUserRole(userId: String, role: UserRole): Result<User> {
    val index = usersList.indexOfFirst { it.id == userId }
    if (index == -1) return Result.failure(IllegalArgumentException("User not found"))
    val updated = usersList[index].copy(role = role)
    usersList[index] = updated
    if (_currentUser.value?.id == userId) {
      _currentUser.value = updated
      _authState.value = AuthState.Authenticated(updated)
    }
    return Result.success(updated)
  }
}
