package com.smartsleep.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {

    @Insert
    suspend fun insert(session: SleepSessionEntity): Long

    @Update
    suspend fun update(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun observeActiveSession(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveSessionOnce(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE isActive = 0 ORDER BY sleepStartEpochMillis DESC")
    fun observeHistory(): Flow<List<SleepSessionEntity>>

    @Query(
        "SELECT * FROM sleep_sessions WHERE isActive = 0 AND ratingScore IS NOT NULL " +
            "ORDER BY sleepStartEpochMillis DESC LIMIT :limit"
    )
    suspend fun getRecentRatedSessions(limit: Int = 20): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getById(id: Long): SleepSessionEntity?
}
