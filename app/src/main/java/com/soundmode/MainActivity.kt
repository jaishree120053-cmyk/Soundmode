package com.soundmode

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.soundmode.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: MapView
    private lateinit var viewModel: LocationViewModel
    private lateinit var adapter: LocationAdapter
    private val mapMarkers = mutableListOf<Marker>()
    private val mapCircles = mutableListOf<Polygon>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        if (!granted) {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, LocationViewModelFactory(this))[LocationViewModel::class.java]
        mapView = binding.mapView
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(37.7749, -122.4194))

        setupSpinner()
        setupRecyclerView()
        setupButtons()
        observeLocations()
    }

    private fun setupSpinner() {
        val items = listOf(
            getString(R.string.sound_mode_ring),
            getString(R.string.sound_mode_vibrate),
            getString(R.string.sound_mode_silent),
            getString(R.string.sound_mode_dnd)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.soundModeSpinner.adapter = adapter
    }

    private fun setupRecyclerView() {
        adapter = LocationAdapter { profile ->
            viewModel.delete(profile)
        }
        binding.locationRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.locationRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        binding.saveLocationButton.setOnClickListener {
            val name = binding.locationNameInput.text?.toString()?.trim().orEmpty()
            val radius = binding.radiusInput.text?.toString()?.toIntOrNull() ?: 150
            if (name.isBlank()) {
                Toast.makeText(this, R.string.location_name_hint, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val center = mapView.mapCenter as? GeoPoint ?: return@setOnClickListener
            val mode = when (binding.soundModeSpinner.selectedItemPosition) {
                1 -> SoundMode.VIBRATE
                2 -> SoundMode.SILENT
                3 -> SoundMode.DND
                else -> SoundMode.RING
            }
            viewModel.save(
                LocationProfile(
                    name = name,
                    latitude = center.latitude,
                    longitude = center.longitude,
                    radiusMeters = radius,
                    soundMode = mode
                )
            )
            binding.locationNameInput.text?.clear()
        }

        binding.requestPermissionButton.setOnClickListener {
            requestLocationPermissions()
        }

        binding.requestDndButton.setOnClickListener {
            requestDndAccess()
        }

        binding.startServiceButton.setOnClickListener {
            toggleService()
        }
    }

    private fun observeLocations() {
        lifecycleScope.launch {
            viewModel.locations.collectLatest { profiles ->
                adapter.submitList(profiles)
                updateMapOverlays(profiles)
            }
        }
    }

    private fun updateMapOverlays(profiles: List<LocationProfile>) {
        mapMarkers.forEach { mapView.overlays.remove(it) }
        mapCircles.forEach { mapView.overlays.remove(it) }
        mapMarkers.clear()
        mapCircles.clear()

        profiles.forEach { profile ->
            val point = GeoPoint(profile.latitude, profile.longitude)
            val marker = Marker(mapView).apply {
                position = point
                title = profile.name
                snippet = "${profile.soundMode.label} • ${profile.radiusMeters}m"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapMarkers.add(marker)
            mapView.overlays.add(marker)

            val circle = Polygon().apply {
                points = Polygon.pointsAsCircle(point, profile.radiusMeters.toDouble())
                fillColor = 0x3300B0FF
                strokeColor = 0xFF00B0FF.toInt()
                strokeWidth = 3f
            }
            mapCircles.add(circle)
            mapView.overlays.add(circle)
        }
        mapView.invalidate()
    }

    private fun requestLocationPermissions() {
        val permissions = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
        permissionLauncher.launch(permissions)
    }

    private fun requestDndAccess() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        } else {
            Toast.makeText(this, R.string.dnd_already_granted, Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleService() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show()
            return
        }
        val isRunning = LocationMonitorService.isRunning
        val intent = Intent(this, LocationMonitorService::class.java)
        if (isRunning) {
            stopService(intent)
            binding.startServiceButton.text = getString(R.string.start_service)
            Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show()
        } else {
            ContextCompat.startForegroundService(this, intent)
            binding.startServiceButton.text = getString(R.string.stop_service)
            Toast.makeText(this, R.string.service_running, Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        updateServiceButton()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    private fun updateServiceButton() {
        binding.startServiceButton.text = if (LocationMonitorService.isRunning) {
            getString(R.string.stop_service)
        } else {
            getString(R.string.start_service)
        }
    }
}
