package com.example.riwaz

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import com.example.riwaz.models.PracticeSession
import com.example.riwaz.models.RagaCategory
import com.example.riwaz.ui.components.*
import com.example.riwaz.ui.theme.RiwazTheme
import com.example.riwaz.utils.EnhancedAudioAnalyzer
import com.example.riwaz.ui.theme.iOSBlack
import com.example.riwaz.ui.theme.SaffronPrimary
import com.example.riwaz.viewmodel.AnalysisViewModel
import com.example.riwaz.viewmodel.RecordingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var audioRecord: AudioRecord? = null
    private var currentRecordingPath: String? = null
    private var isRecordingAudio = false
    private var currentRaga: String = "Yaman" // Default raga

    private val amplitudes = mutableStateListOf<Float>()
    private var recordingJob: Job? = null

    private val saffronColor = SaffronPrimary

    private val ragaCategories = listOf(
        RagaCategory("Morning Ragas", listOf("Bhairav", "Todi")),
        RagaCategory("Afternoon Ragas", listOf("Puriya Dhanashree")), // Approximate classification
        RagaCategory("Evening Ragas", listOf("Yaman", "Bihag", "Bhupali")),
        RagaCategory("Night Ragas", listOf("Malkauns", "Bageshree", "Desh", "Kafi")),
        RagaCategory("All Supported", listOf(
            "Yaman", "Bhairav", "Todi", "Malkauns", "Bhupali",
            "Desh", "Kafi", "Bihag", "Bageshree", "Puriya Dhanashree"
        ))
    )

    private val practiceTypes = listOf(
        "Alaap",
        "Sargam Practice",
        "Taan Practice",
        "Composition",
        "Bandish",
        "Other"
    )

    private val tempoOptions = listOf("Vilambit", "Madhya", "Drut")

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.e("MainActivity", "Audio recording permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            RiwazTheme(darkTheme = true) {
                val recordingViewModel: RecordingViewModel = remember { RecordingViewModel.newInstance(this) }
                val analysisViewModel: AnalysisViewModel = remember { AnalysisViewModel.newInstance(this) }

                val recordings by recordingViewModel.recordings.collectAsState()
                val isRecording by recordingViewModel.isRecording.collectAsState()

                var playingPath by remember { mutableStateOf<String?>(null) }
                var playbackProgress by remember { mutableFloatStateOf(0f) }
                var showSplash by remember { mutableStateOf(true) }
                var showMetadataDialog by remember { mutableStateOf(false) }
                var selectedRaga by remember { mutableStateOf("Yaman") }
                var selectedPracticeType by remember { mutableStateOf("Alaap") }
                var selectedTempo by remember { mutableStateOf("Madhya") }
                var practiceNotes by remember { mutableStateOf("") }

                var analyzingSession by remember { mutableStateOf<PracticeSession?>(null) }

                LaunchedEffect(Unit) {
                    delay(2000)
                    showSplash = false
                }

                LaunchedEffect(playingPath) {
                    if (playingPath != null) {
                        while (playingPath != null && isActive) {
                            player?.let {
                                if (it.isPlaying && it.duration > 0) {
                                    playbackProgress = it.currentPosition.toFloat() / it.duration
                                }
                            }
                            delay(100)
                        }
                    } else {
                        playbackProgress = 0f
                    }
                }

                AnimatedContent(
                    targetState = if (analyzingSession != null) "analysis" else if (showSplash) "splash" else "main",
                    transitionSpec = {
                        when {
                            targetState == "splash" -> {
                                // From main/analysis to splash (shouldn't normally happen, but just in case)
                                fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 300))
                            }
                            initialState == "splash" && targetState == "main" -> {
                                // From splash to main
                                slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
                                ) + fadeIn() togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 300))
                            }
                            targetState == "analysis" -> {
                                // From main to analysis
                                slideInHorizontally(
                                    initialOffsetX = { it },
                                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                ) + fadeIn() togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 2 },
                                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                ) + fadeOut()
                            }
                            initialState == "analysis" && targetState == "main" -> {
                                // From analysis to main
                                slideInHorizontally(
                                    initialOffsetX = { -it },
                                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                ) + fadeIn() togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                ) + fadeOut()
                            }
                            else -> {
                                // Default transition
                                fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 300))
                            }
                        }
                    },
                    label = "screen-transition"
                ) { screen ->
                    when (screen) {
                        "analysis" -> {
                            BackHandler {
                                stopPlaying()
                                playingPath = null
                                analyzingSession = null
                            }
                            AnalysisView(
                                session = analyzingSession!!,
                                isPlaying = playingPath == analyzingSession?.file?.absolutePath,
                                progress = playbackProgress,
                                saffronColor = saffronColor,
                                analysisViewModel = analysisViewModel,
                                onPlayToggle = {
                                    performHaptic()
                                    val path = analyzingSession!!.file.absolutePath
                                    if (playingPath == path) {
                                        stopPlaying()
                                        playingPath = null
                                    } else {
                                        stopPlaying()
                                        playRecording(path) {
                                            playingPath = null
                                        }
                                        playingPath = path
                                    }
                                },
                                onBack = {
                                    stopPlaying()
                                    playingPath = null
                                    analyzingSession = null
                                }
                            )
                        }
                        "splash" -> {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = iOSBlack
                            ) {
                                RiwazSplashScreen(saffronColor)
                            }
                        }
                        else -> {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = iOSBlack
                            ) {
                                RecorderApp(
                                    recordings = recordings,
                                    isRecording = isRecording,
                                    playingPath = playingPath,
                                    playbackProgress = playbackProgress,
                                    amplitudes = amplitudes,
                                    selectedRaga = selectedRaga,
                                    selectedPracticeType = selectedPracticeType,
                                    selectedTempo = selectedTempo,
                                    saffronColor = saffronColor,
                                    recordingViewModel = recordingViewModel,
                                    onRecordToggle = {
                                        performHaptic()
                                        if (!isRecording) {
                                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                showMetadataDialog = true
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        } else {
                                            stopRecording()
                                            recordingViewModel.stopRecording()
                                            currentRecordingPath?.let { path ->
                                                saveMetadata(currentRecordingPath, selectedRaga, selectedPracticeType, selectedTempo, practiceNotes)
                                                val recordingFile = File(path)
                                                val session = PracticeSession(
                                                    file = recordingFile,
                                                    raga = selectedRaga,
                                                    practiceType = selectedPracticeType,
                                                    tempo = selectedTempo,
                                                    notes = practiceNotes
                                                )
                                                recordingViewModel.saveRecording(session)
                                            }
                                            practiceNotes = ""
                                            // recordings will be updated via the flow
                                        }
                                    },
                                    onPlayToggle = { path ->
                                        performHaptic()
                                        if (playingPath == path) {
                                            stopPlaying()
                                            playingPath = null
                                        } else {
                                            stopPlaying()
                                            playRecording(path) {
                                                playingPath = null
                                            }
                                            playingPath = path
                                        }
                                    },
                                    onDeleteRecording = { session ->
                                        recordingViewModel.deleteRecording(session)
                                    },
                                    onAnalyzeRecording = { session ->
                                        analyzingSession = session
                                    }
                                )
                            }
                        }
                    }
                }

                if (showMetadataDialog) {
                    SessionDetailsScreen(
                        ragaCategories = ragaCategories,
                        practiceTypes = practiceTypes,
                        tempoOptions = tempoOptions,
                        selectedRaga = selectedRaga,
                        selectedPracticeType = selectedPracticeType,
                        selectedTempo = selectedTempo,
                        notes = practiceNotes,
                        saffronColor = saffronColor,
                        onRagaChange = { selectedRaga = it },
                        onPracticeTypeChange = { selectedPracticeType = it },
                        onTempoChange = { selectedTempo = it },
                        onNotesChange = { practiceNotes = it },
                        onSave = {
                            showMetadataDialog = false
                            startRecording()
                            recordingViewModel.startRecording()
                        },
                        onCancel = { showMetadataDialog = false }
                    )
                }
            }
        }
    }

    private fun saveMetadata(path: String?, raga: String, type: String, tempo: String, notes: String) {
        path?.let {
            val metadataFile = File(it.replace(".m4a", ".meta"))
            try {
                val safeNotes = notes.replace("|", " ")
                metadataFile.writeText("$raga|$type|$tempo|$safeNotes")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to save metadata", e)
            }
        }
    }

    private fun loadMetadata(file: File): PracticeSession {
        val metadataFile = File(file.absolutePath.replace(".m4a", ".meta"))
        return try {
            if (metadataFile.exists()) {
                val parts = metadataFile.readText().split("|")
                PracticeSession(
                    file = file,
                    raga = parts.getOrNull(0) ?: "Unknown Raga",
                    practiceType = parts.getOrNull(1) ?: "Practice",
                    tempo = parts.getOrNull(2) ?: "Madhya",
                    notes = parts.getOrNull(3) ?: ""
                )
            } else {
                PracticeSession(file, "Unknown Raga", "Practice", "Madhya", "")
            }
        } catch (e: Exception) {
            PracticeSession(file, "Unknown Raga", "Practice", "Madhya", "")
        }
    }

    private fun performHaptic() {
        try {
            val vibrator = getSystemService<Vibrator>()
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(10)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Haptic feedback failed", e)
        }
    }

    private fun getRecordings(): List<PracticeSession> {
        val files = filesDir.listFiles { file -> file.extension == "m4a" }
        return files?.sortedByDescending { it.lastModified() }?.map { loadMetadata(it) } ?: emptyList()
    }

    private fun startRecording() {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(filesDir, "Riwaz_$timeStamp.m4a")
            currentRecordingPath = file.absolutePath

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                val audioSource = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaRecorder.AudioSource.VOICE_PERFORMANCE
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> MediaRecorder.AudioSource.UNPROCESSED
                    else -> MediaRecorder.AudioSource.VOICE_RECOGNITION
                }
                
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(48000)
                setAudioEncodingBitRate(256000)
                setOutputFile(currentRecordingPath)
                prepare()
                start()
            }

            startAudioMonitoring()
        } catch (e: Exception) {
            Log.e("MainActivity", "Recording failed", e)
            try {
                recorder?.release()
                recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128000)
                    setOutputFile(currentRecordingPath)
                    prepare()
                    start()
                }
                startAudioMonitoring()
            } catch (ex: Exception) {
                Log.e("MainActivity", "Fallback recording also failed", ex)
            }
        }
    }

    private fun startAudioMonitoring() {
        isRecordingAudio = true
        amplitudes.clear()

        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 48000
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (bufferSize <= 0) return@launch

            try {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return@launch
                }

                val audioSource = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaRecorder.AudioSource.VOICE_PERFORMANCE
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> MediaRecorder.AudioSource.UNPROCESSED
                    else -> MediaRecorder.AudioSource.VOICE_RECOGNITION
                }

                audioRecord = AudioRecord(
                    audioSource,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                }

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    return@launch
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize)
                // Pass context to enable ML models
                val audioAnalyzer = EnhancedAudioAnalyzer(sampleRate, this@MainActivity)

                while (isRecordingAudio && isActive) {
                    val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (read > 0) {
                        // Calculate amplitude (volume)
                        var sum = 0L
                        for (i in 0 until read) {
                            sum += abs(buffer[i].toInt())
                        }
                        val amplitude = (sum.toFloat() / read) / 32768f

                        // Convert to float array for processing
                        val floatBuffer = buffer.take(read).map { it.toFloat() / Short.MAX_VALUE }.toFloatArray()

                        // Detect pitch in the current buffer using enhanced analyzer
                        val pitch = audioAnalyzer.detectPitch(floatBuffer, sampleRate)

                        // Identify the closest swar based on the detected pitch
                        val detectedSwar = if (pitch > 0) {
                            identifySwarFromPitch(pitch)
                        } else {
                            "Silence"
                        }

                        // Calculate accuracy based on the detected swar
                        val accuracy = calculateSwarAccuracy(pitch, currentRaga)

                        withContext(Dispatchers.Main) {
                            if (amplitudes.size >= 30) {
                                amplitudes.removeAt(0)
                            }
                            amplitudes.add(amplitude)

                            // The real-time feedback will be handled by the composable layer
                            // that has access to the ViewModel
                        }
                    }
                    delay(50)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Audio monitoring failed", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error stopping audioRecord", e)
                }
                audioRecord = null
            }
        }
    }

    /**
     * Identifies the closest swar based on the detected pitch frequency
     */
    private fun identifySwarFromPitch(pitch: Float): String {
        val swarFrequencies = mapOf(
            "Sa" to 261.63f,      // C
            "Re(k)" to 275.71f,   // C# (Komala Re)
            "Re" to 293.66f,      // D (Shuddha Re)
            "Ga(k)" to 309.23f,   // D# (Komala Ga)
            "Ga" to 329.63f,      // E (Shuddha Ga)
            "Ma" to 349.23f,      // F (Shuddha Ma)
            "Ma(t)" to 370.79f,   // F# (Tivra Ma)
            "Pa" to 392.00f,      // G (Panchama)
            "Dha(k)" to 413.41f,  // G# (Komala Dha)
            "Dha" to 440.00f,     // A (Shuddha Dha)
            "Ni(k)" to 466.16f,   // A# (Komala Ni)
            "Ni" to 493.88f       // B (Shuddha Ni)
        )

        var closestSwar = "Silence"
        var minDiff = Float.MAX_VALUE

        for ((swar, freq) in swarFrequencies) {
            val diff = kotlin.math.abs(freq - pitch)
            if (diff < minDiff) {
                minDiff = diff
                closestSwar = swar
            }
        }

        // Only return the swar if it's within a reasonable tolerance (50 cents)
        return if (minDiff < 50) closestSwar else "Off-Pitch"
    }

    /**
     * Calculates the accuracy of a detected swar based on the raga
     */
    private fun calculateSwarAccuracy(pitch: Float, raga: String): Float {
        if (pitch <= 0) return 0f

        val detectedSwar = identifySwarFromPitch(pitch)

        // Define raga-specific swars
        val ragaSwars = when (raga) {
            "Yaman" -> listOf("Sa", "Re", "Ga", "Ma(t)", "Pa", "Dha", "Ni")
            "Bhairav" -> listOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha(k)", "Ni")
            "Todi" -> listOf("Sa", "Re(k)", "Ga(k)", "Ma(t)", "Pa", "Dha(k)", "Ni")
            "Malkauns" -> listOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)")
            "Bhupali" -> listOf("Sa", "Re", "Ga", "Pa", "Dha")
            else -> listOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni")
        }

        // Calculate accuracy based on whether the detected swar is in the raga
        val isSwarInRaga = ragaSwars.contains(detectedSwar)

        // Calculate pitch accuracy (how close to the expected frequency)
        val expectedFreq = when (detectedSwar) {
            "Sa" -> 261.63f
            "Re(k)" -> 275.71f
            "Re" -> 293.66f
            "Ga(k)" -> 309.23f
            "Ga" -> 329.63f
            "Ma" -> 349.23f
            "Ma(t)" -> 370.79f
            "Pa" -> 392.00f
            "Dha(k)" -> 413.41f
            "Dha" -> 440.00f
            "Ni(k)" -> 466.16f
            "Ni" -> 493.88f
            else -> 261.63f
        }

        val pitchDeviation = if (expectedFreq > 0) {
            kotlin.math.abs(expectedFreq - pitch) / expectedFreq
        } else {
            1f // Maximum deviation if no expected frequency
        }

        // Calculate overall accuracy (0.0 to 1.0)
        val pitchAccuracy = (1 - (pitchDeviation * 2)).coerceIn(0f, 1f) // Max 50% deviation allowed
        val ragaAccuracy = if (isSwarInRaga) 1f else 0.2f // Lower accuracy if swar not in raga

        return (pitchAccuracy * 0.7f + ragaAccuracy * 0.3f) // Weighted combination
    }

    private fun stopRecording() {
        isRecordingAudio = false
        recordingJob?.cancel()

        try {
            recorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.e("MainActivity", "MediaRecorder stop failed", e)
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Stop recording failed", e)
        }
        recorder = null

        // Ensure the recording file exists before saving metadata
        currentRecordingPath?.let { path ->
            val recordingFile = File(path)
            if (recordingFile.exists()) {
                Log.d("MainActivity", "Recording file saved successfully: ${recordingFile.absolutePath}, size: ${recordingFile.length()} bytes")
            } else {
                Log.e("MainActivity", "Recording file does not exist after recording: $path")
            }
        }

        amplitudes.clear()
    }

    private fun playRecording(path: String, onComplete: () -> Unit) {
        try {
            val file = File(path)
            if (!file.exists()) {
                onComplete()
                return
            }

            player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
                setOnCompletionListener {
                    stopPlaying()
                    onComplete()
                }
            }
        } catch (e: Exception) {
            onComplete()
        }
    }

    private fun stopPlaying() {
        try {
            player?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Stop playing failed", e)
        }
        player = null
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecordingAudio = false
        recordingJob?.cancel()
        stopRecording()
        stopPlaying()
    }
}
