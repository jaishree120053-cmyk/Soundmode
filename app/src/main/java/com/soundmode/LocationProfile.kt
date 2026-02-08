package com.soundmode

data class LocationProfile(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val soundMode: SoundMode
)
