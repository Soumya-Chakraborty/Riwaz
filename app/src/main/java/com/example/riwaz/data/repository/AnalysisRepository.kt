package com.example.riwaz.data.repository

import com.example.riwaz.models.PracticeSession
import com.example.riwaz.ui.components.AnalysisData
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    suspend fun analyzeRecording(session: PracticeSession, scale: String = "C (261.63 Hz)"): AnalysisData
    suspend fun getAnalysisHistory(sessionId: String): List<AnalysisData>
}