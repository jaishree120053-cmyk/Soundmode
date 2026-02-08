package com.soundmode

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [LocationProfile::class], version = 1)
@TypeConverters(SoundModeConverters::class)
abstract class SoundmodeDatabase : RoomDatabase() {
    abstract fun locationProfileDao(): LocationProfileDao

    companion object {
        @Volatile
        private var instance: SoundmodeDatabase? = null

        fun getInstance(context: Context): SoundmodeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoundmodeDatabase::class.java,
                    "soundmode.db"
                ).build().also { instance = it }
            }
    }
}
