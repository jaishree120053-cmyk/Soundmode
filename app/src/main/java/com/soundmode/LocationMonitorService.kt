package com.soundmode

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var repository: LocationRepository
    private var lastAppliedMode: SoundMode? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            evaluateLocation(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val db = SoundmodeDatabase.getInstance(this)
        repository = LocationRepository(db.locationProfileDao())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun evaluateLocation(location: Location) {
        serviceScope.launch {
            val profiles = repository.getAllOnce()
            var closestProfile: LocationProfile? = null
            var closestDistance = Double.MAX_VALUE
            profiles.forEach { profile ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude,
                    location.longitude,
                    profile.latitude,
                    profile.longitude,
                    results
                )
                val distance = results.firstOrNull()?.toDouble() ?: Double.MAX_VALUE
                if (distance <= profile.radiusMeters && distance < closestDistance) {
                    closestDistance = distance
                    closestProfile = profile
                }
            }
            val targetMode = closestProfile?.soundMode ?: SoundMode.RING
            if (targetMode != lastAppliedMode) {
                applySoundMode(targetMode)
                lastAppliedMode = targetMode
            }
        }
    }

    private fun applySoundMode(mode: SoundMode) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val notificationManager = getSystemService(NotificationManager::class.java)
        when (mode) {
            SoundMode.RING -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            SoundMode.VIBRATE -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            SoundMode.SILENT -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
            SoundMode.DND -> {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val channelId = "soundmode_location"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Soundmode monitoring")
            .setContentText("Location-based sound mode automation is active.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        var isRunning: Boolean = false
            private set
    }
}
