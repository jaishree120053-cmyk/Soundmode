package com.soundmode

class AutomationEngine(
    private val repository: LocationRepository = LocationRepository()
) {
    fun evaluateLocation(latitude: Double, longitude: Double): SoundMode? {
        return repository.getProfiles().firstOrNull { profile ->
            val distance = LocationUtils.distanceMeters(
                latitude,
                longitude,
                profile.latitude,
                profile.longitude
            )
            distance <= profile.radiusMeters
        }?.soundMode
    }
}
