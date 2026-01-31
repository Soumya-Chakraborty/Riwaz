package com.example.riwaz.data

import com.example.riwaz.utils.ScaleManager

/**
 * Data class to hold user preferences for the Riwaz application
 */
data class UserPreferences(
    val referenceScale: String = ScaleManager.getDefaultScale().name,
    val pitchAccuracyThreshold: Float = 0.7f,
    val rhythmAccuracyThreshold: Float = 0.6f,
    val stabilityThreshold: Float = 0.7f,
    val vibratoThreshold: Float = 0.6f,
    val showPitchFeedback: Boolean = true,
    val showRhythmFeedback: Boolean = true,
    val showVocalRangeFeedback: Boolean = true,
    val practiceReminderEnabled: Boolean = false,
    val practiceReminderTime: String = "09:00",
    val weeklyGoalMinutes: Int = 300, // 5 hours per week
    val preferredRagas: List<String> = emptyList(),
    val skillLevel: SkillLevel = SkillLevel.INTERMEDIATE
)

/**
 * Enum representing different skill levels
 */
enum class SkillLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED
}

/**
 * Extension function to get default thresholds based on skill level
 */
fun SkillLevel.getDefaultThresholds(): Pair<Float, Float> {
    return when (this) {
        SkillLevel.BEGINNER -> Pair(0.5f, 0.4f) // Lower accuracy, more forgiving
        SkillLevel.INTERMEDIATE -> Pair(0.7f, 0.6f) // Moderate accuracy
        SkillLevel.ADVANCED -> Pair(0.85f, 0.75f) // Higher accuracy expectations
    }
}