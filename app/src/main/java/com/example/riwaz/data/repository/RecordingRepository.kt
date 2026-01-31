package com.example.riwaz.data.repository

import com.example.riwaz.models.PracticeSession
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getRecordings(): Flow<List<PracticeSession>>
    suspend fun saveRecording(session: PracticeSession)
    suspend fun deleteRecording(session: PracticeSession)
    suspend fun getRecordingById(id: String): PracticeSession?
}