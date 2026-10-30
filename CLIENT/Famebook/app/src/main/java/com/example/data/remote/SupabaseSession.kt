package com.example.data.remote

import com.example.domain.model.User

/** Shared in-process auth state for repositories using the same Supabase session. */
class SupabaseSession {
  @Volatile var accessToken: String? = null
  @Volatile var refreshToken: String? = null
  @Volatile var currentUser: User? = null
}
