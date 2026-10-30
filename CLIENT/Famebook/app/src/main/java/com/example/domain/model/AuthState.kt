package com.example.domain.model

sealed class AuthState {
  data object Unauthenticated : AuthState()
  data object Authenticating : AuthState()
  data class Authenticated(val user: User) : AuthState()
  data class AuthenticationError(val message: String) : AuthState()
}
