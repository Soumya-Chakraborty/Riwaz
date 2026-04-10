package com.example.riwaz.data.repository.impl

import android.content.Context
import com.example.riwaz.data.repository.AnalysisRepository
import com.example.riwaz.models.PracticeSession
import com.example.riwaz.ui.components.AnalysisData
import com.example.riwaz.utils.AudioProcessor

class AnalysisRepositoryImpl(private val context: Context) : AnalysisRepository {

    // Context-aware AudioProcessor unlocks ML/HMM analysis via MLModelManager
    private val audioProcessor = AudioProcessor(context)

    override suspend fun analyzeRecording(session: PracticeSession, scale: String): AnalysisData {
        // Run full ML-backed analysis including the HMM sequence model
        return AnalysisData.from(session, scale, audioProcessor)
    }

    override suspend fun getAnalysisHistory(sessionId: String): List<AnalysisData> {
        return emptyList()
    }
}