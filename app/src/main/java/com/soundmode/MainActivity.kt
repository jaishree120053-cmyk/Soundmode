package com.soundmode

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soundmode.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository = LocationRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderLocations()
        bindActions()
    }

    private fun renderLocations() {
        val locations = repository.getProfiles()
        binding.locationList.text = locations.joinToString(separator = "\n") {
            "${it.name} • ${it.soundMode.label}"
        }
    }

    private fun bindActions() {
        binding.addLocationButton.setOnClickListener {
            Toast.makeText(this, R.string.add_location, Toast.LENGTH_SHORT).show()
        }
        binding.startServiceButton.setOnClickListener {
            Toast.makeText(this, R.string.start_service, Toast.LENGTH_SHORT).show()
        }
    }
}
