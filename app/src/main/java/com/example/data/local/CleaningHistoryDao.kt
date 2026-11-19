package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CleaningHistoryDao {
    @Query("SELECT * FROM cleaning_history ORDER BY timestampMillis DESC")
    fun getAllHistory(): Flow<List<CleaningHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: CleaningHistoryEntity)

    @Query("DELETE FROM cleaning_history")
    suspend fun clearAllHistory()
}
