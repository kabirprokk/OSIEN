package com.example.data.repository

import com.example.data.local.LocalSeedData
import com.example.domain.model.CrewProfile
import com.example.domain.model.CrewRole
import com.example.domain.repository.CrewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Production-ready local implementation of CrewRepository.
 * Manages verified FameBros studio crew profiles and availability.
 */
class LocalCrewRepository : CrewRepository {
  private val _crewProfiles = MutableStateFlow<List<CrewProfile>>(LocalSeedData.initialCrewProfiles)
  override val crewProfiles: StateFlow<List<CrewProfile>> = _crewProfiles.asStateFlow()

  override fun getCrewProfile(crewId: String): Flow<CrewProfile?> {
    return _crewProfiles.map { profiles -> profiles.find { it.id == crewId || it.userId == crewId } }
  }

  override fun getCrewProfileByUserId(userId: String): Flow<CrewProfile?> {
    return _crewProfiles.map { profiles -> profiles.find { it.userId == userId } }
  }

  override suspend fun setAvailability(crewId: String, isAvailable: Boolean): Result<Unit> {
    _crewProfiles.value = _crewProfiles.value.map { profile ->
      if (profile.id == crewId || profile.userId == crewId) {
        profile.copy(isAvailable = isAvailable)
      } else {
        profile
      }
    }
    return Result.success(Unit)
  }

  override fun getAvailableCrew(role: CrewRole?): List<CrewProfile> {
    return _crewProfiles.value.filter { profile ->
      profile.isAvailable && (role == null || profile.primaryRole == role || profile.secondaryRoles.contains(role))
    }
  }

  override suspend fun updateCrewProfile(profile: CrewProfile): Result<CrewProfile> {
    val current = _crewProfiles.value
    val index = current.indexOfFirst { it.id == profile.id || it.userId == profile.userId }
    _crewProfiles.value = if (index != -1) {
      current.toMutableList().apply { set(index, profile) }
    } else {
      current + profile
    }
    return Result.success(profile)
  }
}
