package com.soundmode

class LocationRepository {

    fun getProfiles(): List<LocationProfile> {
        return listOf(
            LocationProfile("Home", 37.4219983, -122.084, 150f, SoundMode.VIBRATE),
            LocationProfile("College", 37.430, -122.173, 200f, SoundMode.RING),
            LocationProfile("Office", 37.331, -122.030, 120f, SoundMode.DO_NOT_DISTURB)
        )
    }
}
