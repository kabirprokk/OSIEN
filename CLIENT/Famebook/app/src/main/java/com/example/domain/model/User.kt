package com.example.domain.model

enum class UserRole {
  CLIENT,
  CREW,
  ADMIN
}

data class User(
  val id: String,
  val name: String,
  val email: String,
  val phone: String,
  val role: UserRole,
  val avatarUrl: String? = null,
  val companyName: String? = null,
  val bio: String = ""
)

data class AuthSession(
  val userId: String,
  val token: String,
  val role: UserRole,
  val createdAt: Long = System.currentTimeMillis()
)

data class UserProfile(
  val userId: String,
  val fullName: String,
  val email: String,
  val phone: String,
  val role: UserRole,
  val companyName: String? = null,
  val bio: String = ""
)

