package com.soundmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LocationViewModel(private val repository: LocationRepository) : ViewModel() {
    val locations: StateFlow<List<LocationProfile>> = repository.locations
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun save(profile: LocationProfile) {
        viewModelScope.launch {
            repository.save(profile)
        }
    }

    fun delete(profile: LocationProfile) {
        viewModelScope.launch {
            repository.delete(profile)
        }
    }
}
