package com.example.riwaz.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.riwaz.data.repository.AnalysisRepository
import com.example.riwaz.data.repository.RecordingRepository
import com.example.riwaz.data.repository.impl.AnalysisRepositoryImpl
import com.example.riwaz.data.repository.impl.RecordingRepositoryImpl
import com.example.riwaz.models.PracticeSession
import com.example.riwaz.ui.components.AnalysisData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecordingViewModel(
    private val recordingRepository: RecordingRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    companion object {
        @JvmStatic
        fun newInstance(context: android.content.Context): RecordingViewModel {
            return RecordingViewModel(
                RecordingRepositoryImpl(context),
                AnalysisRepositoryImpl(context)
            )
        }
    }

    private val _recordings = MutableStateFlow<List<PracticeSession>>(emptyList())
    val recordings: StateFlow<List<PracticeSession>> = _recordings.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentRecordingPath = MutableStateFlow<String?>(null)
    val currentRecordingPath: StateFlow<String?> = _currentRecordingPath.asStateFlow()

    private val _currentPitch = MutableStateFlow<Float>(0f)
    val currentPitch: StateFlow<Float> = _currentPitch.asStateFlow()

    private val _currentSwar = MutableStateFlow<String>("Silence")
    val currentSwar: StateFlow<String> = _currentSwar.asStateFlow()

    private val _currentAccuracy = MutableStateFlow<Float>(0f)
    val currentAccuracy: StateFlow<Float> = _currentAccuracy.asStateFlow()

    init {
        viewModelScope.launch {
            loadRecordings()
        }
    }

    private fun loadRecordings() {
        viewModelScope.launch {
            recordingRepository.getRecordings().collect { sessions ->
                _recordings.value = sessions
            }
        }
    }

    fun startRecording() {
        _isRecording.value = true
        // In a real implementation, this would trigger the actual recording
    }

    fun stopRecording() {
        _isRecording.value = false
        // In a real implementation, this would stop the actual recording
    }

    fun saveRecording(session: PracticeSession) {
        viewModelScope.launch {
            recordingRepository.saveRecording(session)
            loadRecordings() // Refresh the list
        }
    }

    fun deleteRecording(session: PracticeSession) {
        viewModelScope.launch {
            recordingRepository.deleteRecording(session)
            loadRecordings() // Refresh the list
        }
    }

    fun analyzeRecording(session: PracticeSession) {
        viewModelScope.launch {
            val data = analysisRepository.analyzeRecording(session)
            // Handle the result as needed
        }
    }

    fun updateRealTimeFeedback(pitch: Float, swar: String, accuracy: Float) {
        _currentPitch.value = pitch
        _currentSwar.value = swar
        _currentAccuracy.value = accuracy
    }
}