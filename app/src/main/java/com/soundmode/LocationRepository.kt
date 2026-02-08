package com.soundmode

import kotlinx.coroutines.flow.Flow

class LocationRepository(private val dao: LocationProfileDao) {
    val locations: Flow<List<LocationProfile>> = dao.getAll()

    suspend fun save(profile: LocationProfile) {
        dao.insert(profile)
    }

    suspend fun delete(profile: LocationProfile) {
        dao.delete(profile)
    }

    suspend fun getAllOnce(): List<LocationProfile> = dao.getAllOnce()
}
