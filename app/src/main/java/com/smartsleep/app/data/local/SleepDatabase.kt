package com.smartsleep.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SleepSessionEntity::class], version = 1, exportSchema = false)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao

    companion object {
        @Volatile private var INSTANCE: SleepDatabase? = null

        fun getInstance(context: Context): SleepDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SleepDatabase::class.java,
                    "smart_sleep.db"
                ).build().also { INSTANCE = it }
            }
    }
}
