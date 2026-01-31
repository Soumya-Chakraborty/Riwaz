# Riwaz App: Audio Analysis Algorithm Upgrade Implementation Guide

This document outlines the implementation of state-of-the-art audio analysis algorithms for the Riwaz app, specifically optimized for Indian classical music analysis. The upgrades include enhanced pitch detection, ML-based swar recognition, advanced stability analysis, sophisticated raga validation, modern noise reduction, and improved tonic detection.

## Components Implemented

### 1. YIN Pitch Detection Algorithm (`YinPitchDetector.kt`)
- Implements the YIN algorithm for robust pitch detection
- Optimized for Indian classical music with cultural sensitivity
- Sub-sample accuracy through parabolic interpolation
- Real-time processing optimizations

### 2. ML-Enhanced Swar Recognition (`MLSwarRecognizer.kt`)
- TensorFlow Lite integration for swar classification
- Cultural context awareness for raga-specific recognition
- Fallback to traditional methods if ML model unavailable
- Model file loaded from Android assets

### 3. Advanced Stability Analysis (`StabilityAnalyzer.kt`)
- Multi-metric stability assessment (pitch, amplitude, harmonic)
- Coefficient of variation for stability measurement
- Weighted combination of stability metrics
- Indian classical music specific thresholds

### 4. Sophisticated Raga Validation (`RagaValidator.kt`)
- Characteristic phrase (pakad) recognition
- Forbidden transition detection
- Note hierarchy evaluation
- Movement pattern validation
- Culturally authentic raga rules

### 5. Modern Noise Reduction (`NoiseReducer.kt`)
- Spectral subtraction technique
- Wiener filtering post-processing
- Adaptive noise cancellation
- Hanning windowing for artifact reduction

### 6. Improved Tonic Detection (`TonicDetector.kt`)
- Histogram-based frequency analysis
- Harmonic relationship analysis
- Stability-based validation
- Combined multi-method approach

### 7. Enhanced Audio Analyzer (`EnhancedAudioAnalyzer.kt`)
- Integration of all components
- Comprehensive analysis results
- ML-enhanced recognition when available
- Real-time processing capabilities

## Key Features

### YIN Pitch Detection
```kotlin
fun detectPitch(audioData: FloatArray, sampleRate: Int): Float {
    val yinDetector = YinPitchDetector(sampleRate)
    return yinDetector.detectPitchOptimized(audioData)
}
```

### ML-Based Swar Recognition
```kotlin
fun detectSwarSequenceWithML(
    context: Context,
    audioData: FloatArray, 
    sampleRate: Int, 
    raga: String
): List<SwarData> {
    val mlSwarRecognizer = MLSwarRecognizer(context, "swar_recognition_model.tflite")
    // Use ML recognition if available, fallback to traditional
}
```

### Advanced Stability Analysis
```kotlin
fun analyzeStability(
    audioData: FloatArray,
    sampleRate: Int,
    fundamentalFreq: Float
): StabilityMetrics {
    val pitchStability = calculatePitchStability(audioData, sampleRate, fundamentalFreq)
    val amplitudeStability = calculateAmplitudeStability(audioData)
    val harmonicConsistency = calculateHarmonicConsistency(audioData, fundamentalFreq, sampleRate)
    // Combine metrics with weighted average
}
```

### Raga Validation
```kotlin
fun validateRagaCompliance(
    detectedSwars: List<String>,
    raga: String,
    detectedFrequencies: List<Float> = emptyList()
): RagaValidationResult {
    val notePresence = validateNotePresence(detectedSwars, ragaDefinition.allowedNotes)
    val forbiddenTransitions = checkForbiddenTransitions(detectedSwars, ragaDefinition.forbiddenTransitions)
    // Additional validation checks...
}
```

## Data Models

### Swar Data Structure
```kotlin
data class SwarData(
    val name: String,           // Swar name (e.g., "Sa", "Re", "Ga(k)")
    val accuracy: Float,        // Accuracy score (0.0-1.0)
    val isMistake: Boolean,     // Whether this is considered a mistake
    val expectedFreq: Float,    // Expected frequency in Hz
    val detectedFreq: Float,    // Detected frequency in Hz
    val stability: Float        // Stability score (0.0-1.0)
)
```

### Enhanced Analysis Result
```kotlin
data class EnhancedAnalysisResult(
    val swarData: List<SwarData>,
    val ragaValidation: RagaValidationResult,
    val tonic: TonicDetectionResult,
    val overallScore: Float
)
```

## Integration Points

### In MainActivity
```kotlin
val audioAnalyzer = EnhancedAudioAnalyzer()
val pitch = audioAnalyzer.detectPitch(floatBuffer, sampleRate)
```

### In AudioProcessor
```kotlin
private val audioAnalyzer = EnhancedAudioAnalyzer()

suspend fun analyzeRecording(
    file: File,
    raga: String,
    scale: String = "C (261.63 Hz)"
): List<SwarData> {
    val audioData = readAudioFromFile(file)
    val result = audioAnalyzer.analyzeRecording(audioData, SAMPLE_RATE, raga, scale)
    return result.swarData
}
```

## TensorFlow Lite Dependencies (build.gradle)

```kotlin
// TensorFlow Lite for ML-based swar recognition
implementation('org.tensorflow:tensorflow-lite:2.15.0')
implementation('org.tensorflow:tensorflow-lite-support:0.4.4')
implementation('org.tensorflow:tensorflow-lite-metadata:0.4.4')
```

## Model File Location

The TensorFlow Lite model is expected at:
```
app/src/main/assets/swar_recognition_model.tflite
```

## Performance Considerations

- Real-time processing optimizations with efficient windowing
- Memory-efficient buffer management
- Asynchronous processing for heavy computations
- Fallback mechanisms when ML models are unavailable

## Cultural Authenticity

- Respectful implementation of Indian classical music theory
- Accurate representation of microtonal variations (komal, tivra notes)
- Raga-specific rules and constraints
- Traditional terminology and concepts preserved

## Testing Strategy

- Unit tests for individual components
- Integration tests for full analysis pipeline
- Performance benchmarks for real-time processing
- Accuracy validation against known reference recordings

## Future Enhancements

- Continuous learning from user practice sessions
- Personalized feedback based on individual progress
- Advanced ornamentation detection (meend, gamak, etc.)
- Multi-voice separation for accompaniment analysis

This upgrade transforms the Riwaz app's audio analysis capabilities to state-of-the-art levels while maintaining mobile efficiency and cultural authenticity for Indian classical music education.