package com.soundmode

import androidx.room.TypeConverter

class SoundModeConverters {
    @TypeConverter
    fun fromSoundMode(mode: SoundMode): String = mode.name

    @TypeConverter
    fun toSoundMode(value: String): SoundMode = SoundMode.valueOf(value)
}
