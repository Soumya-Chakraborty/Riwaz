package com.example.riwaz.utils

/**
 * Data classes for audio analysis results
 */

/**
 * Data class for swar analysis results
 */
data class SwarData(
    val name: String,
    val accuracy: Float,
    val isMistake: Boolean = false,
    val expectedFreq: Float = 0f,
    val detectedFreq: Float = 0f,
    val stability: Float = 0.9f
)

/**
 * Data class for error details
 */
data class ErrorDetail(
    val category: ErrorCategory,
    val swar: String,
    val severity: ErrorSeverity,
    val description: String,
    val correction: String
)

enum class ErrorCategory { PITCH, EXPRESSION, TIMING }
enum class ErrorSeverity { CRITICAL, MAJOR, MINOR }

/**
 * Data class for stability metrics (enhanced with SOTA analysis fields)
 */
data class StabilityMetrics(
    val pitchStability: Float,
    val amplitudeStability: Float,
    val harmonicConsistency: Float,
    val overallStability: Float,
    // SOTA additions
    val hnr: Float = 0f,                    // Harmonic-to-Noise Ratio (dB)
    val spectralCentroid: Float = 0f,       // Brightness indicator (Hz)
    val vibratoRate: Float = 0f,            // Vibrato frequency (Hz)
    val vibratoExtent: Float = 0f,          // Vibrato extent (cents)
    val intonationScore: Float = 0f,        // Shruti precision (0-1)
    val voiceQuality: VoiceQuality = VoiceQuality.UNKNOWN
)

/**
 * Voice quality assessment
 */
enum class VoiceQuality {
    EXCELLENT,  // HNR > 20dB, stable pitch
    GOOD,       // HNR > 12dB
    FAIR,       // HNR > 6dB
    POOR,       // HNR < 6dB
    UNKNOWN
}

/**
 * Data class for swar recognition results
 */
data class SwarRecognitionResult(
    val swar: String,
    val confidence: Float,
    val isCulturalContextValid: Boolean
)

/**
 * Data class for raga validation results
 */
data class RagaValidationResult(
    val isCompliant: Boolean,
    val complianceScore: Float,
    val notePresence: Float,
    val forbiddenTransitions: List<String>,
    val characteristicPhrases: Float,
    val noteHierarchy: Float,
    val movementPatterns: Float,
    val suggestions: List<String>
)

/**
 * Data class for tonic detection results
 */
data class TonicDetectionResult(
    val frequency: Float,
    val confidence: Float,
    val method: String
)

/**
 * Data class for enhanced analysis result with comprehensive data
 */
data class EnhancedAnalysisResult(
    val swarData: List<SwarData>,
    val ragaValidation: RagaValidationResult,
    val tonic: TonicDetectionResult,
    val overallScore: Float
)

/**
 * Data class for raga definition
 */
data class RagaDefinition(
    val allowedNotes: List<String>,
    val forbiddenTransitions: List<Pair<String, String>>,
    val characteristicPhrases: List<List<String>>,
    val noteImportance: Map<String, Float>,
    val movementPatterns: List<MovementPattern>
)

/**
 * Data class for movement patterns
 */
data class MovementPattern(
    val type: MovementPatternType,
    val notes: List<String>
)

/**
 * Enum for movement pattern types
 */
enum class MovementPatternType {
    AROHA,    // Ascending
    AVAROHA,  // Descending
    SPECIAL   // Special patterns
}