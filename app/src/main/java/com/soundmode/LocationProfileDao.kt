package com.soundmode

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationProfileDao {
    @Query("SELECT * FROM location_profiles ORDER BY name ASC")
    fun getAll(): Flow<List<LocationProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: LocationProfile)

    @Delete
    suspend fun delete(profile: LocationProfile)

    @Query("SELECT * FROM location_profiles")
    suspend fun getAllOnce(): List<LocationProfile>
}
