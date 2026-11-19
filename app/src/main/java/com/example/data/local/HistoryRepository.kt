package com.example.data.local

import kotlinx.coroutines.flow.Flow

class HistoryRepository(
    private val dao: CleaningHistoryDao
) {
    val historyEntries: Flow<List<CleaningHistoryEntity>> = dao.getAllHistory()

    suspend fun recordOperation(
        appName: String,
        packageName: String,
        operationType: String,
        status: String,
        recoveredBytes: Long?,
        details: String
    ) {
        dao.insertHistory(
            CleaningHistoryEntity(
                timestampMillis = System.currentTimeMillis(),
                appName = appName,
                packageName = packageName,
                operationType = operationType,
                status = status,
                recoveredBytes = recoveredBytes,
                details = details
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }
}
