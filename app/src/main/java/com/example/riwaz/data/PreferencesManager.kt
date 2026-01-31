package com.example.riwaz.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manager for user preferences using DataStore
 */
class PreferencesManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
        
        // Preference keys
        private val REFERENCE_SCALE = stringPreferencesKey("reference_scale")
        private val PITCH_ACCURACY_THRESHOLD = floatPreferencesKey("pitch_accuracy_threshold")
        private val RHYTHM_ACCURACY_THRESHOLD = floatPreferencesKey("rhythm_accuracy_threshold")
        private val STABILITY_THRESHOLD = floatPreferencesKey("stability_threshold")
        private val VIBRATO_THRESHOLD = floatPreferencesKey("vibrato_threshold")
        private val SHOW_PITCH_FEEDBACK = booleanPreferencesKey("show_pitch_feedback")
        private val SHOW_RHYTHM_FEEDBACK = booleanPreferencesKey("show_rhythm_feedback")
        private val SHOW_VOCAL_RANGE_FEEDBACK = booleanPreferencesKey("show_vocal_range_feedback")
        private val PRACTICE_REMINDER_ENABLED = booleanPreferencesKey("practice_reminder_enabled")
        private val PRACTICE_REMINDER_TIME = stringPreferencesKey("practice_reminder_time")
        private val WEEKLY_GOAL_MINUTES = intPreferencesKey("weekly_goal_minutes")
        private val PREFERRED_RAGAS = stringSetPreferencesKey("preferred_ragas")
        private val SKILL_LEVEL = stringPreferencesKey("skill_level")
    }
    
    /**
     * Get user preferences as a Flow
     */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                referenceScale = preferences[REFERENCE_SCALE] ?: UserPreferences().referenceScale,
                pitchAccuracyThreshold = preferences[PITCH_ACCURACY_THRESHOLD] ?: UserPreferences().pitchAccuracyThreshold,
                rhythmAccuracyThreshold = preferences[RHYTHM_ACCURACY_THRESHOLD] ?: UserPreferences().rhythmAccuracyThreshold,
                stabilityThreshold = preferences[STABILITY_THRESHOLD] ?: UserPreferences().stabilityThreshold,
                vibratoThreshold = preferences[VIBRATO_THRESHOLD] ?: UserPreferences().vibratoThreshold,
                showPitchFeedback = preferences[SHOW_PITCH_FEEDBACK] ?: UserPreferences().showPitchFeedback,
                showRhythmFeedback = preferences[SHOW_RHYTHM_FEEDBACK] ?: UserPreferences().showRhythmFeedback,
                showVocalRangeFeedback = preferences[SHOW_VOCAL_RANGE_FEEDBACK] ?: UserPreferences().showVocalRangeFeedback,
                practiceReminderEnabled = preferences[PRACTICE_REMINDER_ENABLED] ?: UserPreferences().practiceReminderEnabled,
                practiceReminderTime = preferences[PRACTICE_REMINDER_TIME] ?: UserPreferences().practiceReminderTime,
                weeklyGoalMinutes = preferences[WEEKLY_GOAL_MINUTES] ?: UserPreferences().weeklyGoalMinutes,
                preferredRagas = preferences[PREFERRED_RAGAS]?.toList() ?: UserPreferences().preferredRagas,
                skillLevel = SkillLevel.valueOf(preferences[SKILL_LEVEL] ?: UserPreferences().skillLevel.name)
            )
        }
    
    /**
     * Update reference scale
     */
    suspend fun updateReferenceScale(scale: String) {
        context.dataStore.edit { preferences ->
            preferences[REFERENCE_SCALE] = scale
        }
    }
    
    /**
     * Update pitch accuracy threshold
     */
    suspend fun updatePitchAccuracyThreshold(threshold: Float) {
        context.dataStore.edit { preferences ->
            preferences[PITCH_ACCURACY_THRESHOLD] = threshold
        }
    }
    
    /**
     * Update rhythm accuracy threshold
     */
    suspend fun updateRhythmAccuracyThreshold(threshold: Float) {
        context.dataStore.edit { preferences ->
            preferences[RHYTHM_ACCURACY_THRESHOLD] = threshold
        }
    }
    
    /**
     * Update stability threshold
     */
    suspend fun updateStabilityThreshold(threshold: Float) {
        context.dataStore.edit { preferences ->
            preferences[STABILITY_THRESHOLD] = threshold
        }
    }
    
    /**
     * Update vibrato threshold
     */
    suspend fun updateVibratoThreshold(threshold: Float) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATO_THRESHOLD] = threshold
        }
    }
    
    /**
     * Update skill level
     */
    suspend fun updateSkillLevel(skillLevel: SkillLevel) {
        context.dataStore.edit { preferences ->
            preferences[SKILL_LEVEL] = skillLevel.name
        }
    }
    
    /**
     * Update weekly goal minutes
     */
    suspend fun updateWeeklyGoalMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[WEEKLY_GOAL_MINUTES] = minutes
        }
    }
    
    /**
     * Reset to default preferences
     */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}