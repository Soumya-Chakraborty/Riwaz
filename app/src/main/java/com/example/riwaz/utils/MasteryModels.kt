package com.example.riwaz.utils

import androidx.compose.ui.graphics.Color

/**
 * Levels of mastery in Indian Classical Music.
 */
enum class MasteryLevel(val label: String, val color: Color) {
    NOVICE("Novice", Color(0xFF9E9E9E)),
    SHISHYA("Shishya", Color(0xFF4CAF50)),
    SADHAK("Sadhak", Color(0xFF2196F3)),
    GANDHARVA("Gandharva", Color(0xFFFFC107))
}

/**
 * Achievements or milestones reached during a session.
 *
 * @param poweredByHmm  When true, this milestone was evaluated by the HMM sequence
 *                      engine (e.g. Pakad phrase recognition via DTW).  The UI will
 *                      render a small "Powered by HMM" badge alongside the title.
 */
data class MasteryMilestone(
    val title: String,
    val description: String,
    val isAchieved: Boolean,
    val poweredByHmm: Boolean = false
)