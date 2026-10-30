package com.example.data.repository

import com.example.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory favorites used by unit tests and as a local fallback.
 * Production uses [SupabaseFavoriteRepository].
 */
class LocalFavoriteRepository : FavoriteRepository {
  private val favorites = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

  override fun getFavoriteCrewIds(clientId: String): Flow<List<String>> =
    favorites.map { (it[clientId] ?: emptySet()).toList() }

  override suspend fun isFavorite(clientId: String, crewUserId: String): Boolean =
    favorites.value[clientId]?.contains(crewUserId) == true

  override suspend fun toggleFavorite(clientId: String, crewUserId: String): Result<Boolean> {
    val current = favorites.value[clientId] ?: emptySet()
    val updated = if (current.contains(crewUserId)) current - crewUserId else current + crewUserId
    favorites.value = favorites.value + (clientId to updated)
    return Result.success(updated.contains(crewUserId))
  }
}
