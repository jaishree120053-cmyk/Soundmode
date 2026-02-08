package com.soundmode

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.soundmode.databinding.ItemLocationBinding

class LocationAdapter(
    private val onDelete: (LocationProfile) -> Unit
) : ListAdapter<LocationProfile, LocationAdapter.LocationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationViewHolder(binding, onDelete)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LocationViewHolder(
        private val binding: ItemLocationBinding,
        private val onDelete: (LocationProfile) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(profile: LocationProfile) {
            binding.locationName.text = profile.name
            binding.locationDetails.text =
                "${profile.radiusMeters}m • ${profile.soundMode.label}"
            binding.deleteButton.setOnClickListener { onDelete(profile) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<LocationProfile>() {
        override fun areItemsTheSame(oldItem: LocationProfile, newItem: LocationProfile): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LocationProfile, newItem: LocationProfile): Boolean =
            oldItem == newItem
    }
}
