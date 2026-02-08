package com.soundmode

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_profiles")
data class LocationProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val soundMode: SoundMode
)
