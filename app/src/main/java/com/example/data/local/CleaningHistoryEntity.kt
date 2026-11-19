package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleaning_history")
data class CleaningHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestampMillis: Long,
    val appName: String,
    val packageName: String,
    val operationType: String,
    val status: String,
    val recoveredBytes: Long?,
    val details: String
)
