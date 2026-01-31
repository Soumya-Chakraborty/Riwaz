package com.example.riwaz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riwaz.data.repository.AnalysisRepository
import com.example.riwaz.data.repository.RagaRepository
import com.example.riwaz.data.repository.impl.AnalysisRepositoryImpl
import com.example.riwaz.data.repository.impl.RagaRepositoryImpl
import com.example.riwaz.models.PracticeSession
import com.example.riwaz.ui.components.AnalysisData
import com.example.riwaz.ui.components.RagaRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AnalysisViewModel(
    private val analysisRepository: AnalysisRepository,
    private val ragaRepository: RagaRepository
) : ViewModel() {

    companion object {
        @JvmStatic
        fun newInstance(context: android.content.Context): AnalysisViewModel {
            return AnalysisViewModel(
                AnalysisRepositoryImpl(context),
                RagaRepositoryImpl()
            )
        }
    }

    private val _analysisData = MutableStateFlow<AnalysisData?>(null)
    val analysisData: StateFlow<AnalysisData?> = _analysisData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ragaData = MutableStateFlow<RagaRegistry.RagaData?>(null)
    val ragaData: StateFlow<RagaRegistry.RagaData?> = _ragaData.asStateFlow()

    fun analyzeSession(session: PracticeSession, scale: String = "C (261.63 Hz)") {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = analysisRepository.analyzeRecording(session, scale)
                _analysisData.value = data

                val ragaInfo = ragaRepository.getRagaData(session.raga)
                _ragaData.value = ragaInfo
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getRagaInfo(ragaName: String) {
        viewModelScope.launch {
            val ragaInfo = ragaRepository.getRagaData(ragaName)
            _ragaData.value = ragaInfo
        }
    }
}