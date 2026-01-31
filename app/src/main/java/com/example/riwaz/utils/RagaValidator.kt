package com.example.riwaz.utils

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Sophisticated Raga Validation with Pattern Recognition
 * Uses multiple approaches including characteristic phrases (pakad), note relationships,
 * and melodic movement patterns to validate raga compliance
 */
class RagaValidator {
    
    /**
     * Validates if the detected swars comply with the specified raga
     */
    fun validateRagaCompliance(
        detectedSwars: List<String>,
        raga: String,
        detectedFrequencies: List<Float> = emptyList()
    ): RagaValidationResult {
        val ragaDefinition = getRagaDefinition(raga)
        
        // Check various aspects of raga compliance
        val notePresence = validateNotePresence(detectedSwars, ragaDefinition.allowedNotes)
        val forbiddenTransitions = checkForbiddenTransitions(detectedSwars, ragaDefinition.forbiddenTransitions)
        val characteristicPhrases = checkCharacteristicPhrases(detectedSwars, ragaDefinition.characteristicPhrases)
        val noteHierarchy = evaluateNoteHierarchy(detectedSwars, ragaDefinition.noteImportance)
        val movementPatterns = validateMovementPatterns(detectedSwars, ragaDefinition.movementPatterns)
        
        // Calculate overall compliance score
        val complianceScore = calculateOverallCompliance(
            notePresence,
            forbiddenTransitions,
            characteristicPhrases,
            noteHierarchy,
            movementPatterns
        )
        
        return RagaValidationResult(
            isCompliant = complianceScore > 0.6f, // Threshold for compliance
            complianceScore = complianceScore,
            notePresence = notePresence,
            forbiddenTransitions = forbiddenTransitions,
            characteristicPhrases = characteristicPhrases,
            noteHierarchy = noteHierarchy,
            movementPatterns = movementPatterns,
            suggestions = generateSuggestions(
                detectedSwars,
                ragaDefinition,
                forbiddenTransitions,
                characteristicPhrases
            )
        )
    }
    
    /**
     * Validates if the detected notes are allowed in the raga
     */
    private fun validateNotePresence(detectedSwars: List<String>, allowedNotes: List<String>): Float {
        if (detectedSwars.isEmpty()) return 0f
        
        var validNotes = 0
        for (swar in detectedSwars) {
            if (allowedNotes.contains(swar) || swar == "Silence") {
                validNotes++
            }
        }
        
        return validNotes.toFloat() / detectedSwars.size
    }
    
    /**
     * Checks for forbidden note transitions in the raga
     */
    private fun checkForbiddenTransitions(detectedSwars: List<String>, forbiddenTransitions: List<Pair<String, String>>): List<String> {
        val violations = mutableListOf<String>()
        
        for (i in 0 until detectedSwars.size - 1) {
            val currentSwar = detectedSwars[i]
            val nextSwar = detectedSwars[i + 1]
            
            if (forbiddenTransitions.any { it.first == currentSwar && it.second == nextSwar }) {
                violations.add("Transition from $currentSwar to $nextSwar is forbidden in this raga")
            }
        }
        
        return violations
    }
    
    /**
     * Checks for presence of characteristic phrases (pakad) in the performance
     */
    private fun checkCharacteristicPhrases(detectedSwars: List<String>, characteristicPhrases: List<List<String>>): Float {
        if (detectedSwars.isEmpty() || characteristicPhrases.isEmpty()) return 0f
        
        var phraseMatches = 0
        var totalPhrases = 0
        
        for (phrase in characteristicPhrases) {
            if (phrase.size <= detectedSwars.size) {
                totalPhrases++
                
                // Check if the phrase appears anywhere in the detected sequence
                for (i in 0..(detectedSwars.size - phrase.size)) {
                    val subsequence = detectedSwars.subList(i, i + phrase.size)
                    if (subsequence == phrase) {
                        phraseMatches++
                        break // Count each phrase once
                    }
                }
            }
        }
        
        return if (totalPhrases > 0) phraseMatches.toFloat() / totalPhrases else 0f
    }
    
    /**
     * Evaluates if the important notes of the raga are emphasized appropriately
     */
    private fun evaluateNoteHierarchy(detectedSwars: List<String>, noteImportance: Map<String, Float>): Float {
        if (detectedSwars.isEmpty() || noteImportance.isEmpty()) return 0f
        
        // Count occurrences of each note
        val noteCounts = mutableMapOf<String, Int>()
        for (swar in detectedSwars) {
            noteCounts[swar] = (noteCounts[swar] ?: 0) + 1
        }
        
        // Calculate how well the important notes are emphasized
        var correctlyEmphasized = 0
        var totalImportantNotes = 0
        
        for ((note, importance) in noteImportance) {
            val count = noteCounts[note] ?: 0
            val totalCount = detectedSwars.size
            
            // If this note should be emphasized (high importance), check if it occurs frequently
            if (importance > 0.5f && totalCount > 0) { // Important notes should appear frequently
                totalImportantNotes++
                val noteFrequency = count.toFloat() / totalCount
                if (noteFrequency >= importance * 0.3f) { // At least 30% of importance threshold
                    correctlyEmphasized++
                }
            }
        }
        
        return if (totalImportantNotes > 0) correctlyEmphasized.toFloat() / totalImportantNotes else 1f
    }
    
    /**
     * Validates melodic movement patterns specific to the raga
     */
    private fun validateMovementPatterns(detectedSwars: List<String>, movementPatterns: List<MovementPattern>): Float {
        if (detectedSwars.isEmpty() || movementPatterns.isEmpty()) return 0f
        
        var patternMatches = 0
        var totalPatterns = 0
        
        for (pattern in movementPatterns) {
            totalPatterns++
            
            when (pattern.type) {
                MovementPatternType.AROHA -> {
                    // Check for ascending patterns
                    if (checkArohaPattern(detectedSwars, pattern.notes)) {
                        patternMatches++
                    }
                }
                MovementPatternType.AVAROHA -> {
                    // Check for descending patterns
                    if (checkAvarohaPattern(detectedSwars, pattern.notes)) {
                        patternMatches++
                    }
                }
                MovementPatternType.SPECIAL -> {
                    // Check for special movement patterns
                    if (checkSpecialPattern(detectedSwars, pattern.notes)) {
                        patternMatches++
                    }
                }
            }
        }
        
        return if (totalPatterns > 0) patternMatches.toFloat() / totalPatterns else 0f
    }
    
    /**
     * Checks for ascending (aroha) patterns
     */
    private fun checkArohaPattern(detectedSwars: List<String>, pattern: List<String>): Boolean {
        // Check if the pattern appears in ascending order in the detected sequence
        var patternIndex = 0
        for (swar in detectedSwars) {
            if (patternIndex < pattern.size && swar == pattern[patternIndex]) {
                patternIndex++
            }
        }
        return patternIndex == pattern.size
    }
    
    /**
     * Checks for descending (avaroha) patterns
     */
    private fun checkAvarohaPattern(detectedSwars: List<String>, pattern: List<String>): Boolean {
        // Check if the pattern appears in descending order in the detected sequence
        var patternIndex = 0
        for (swar in detectedSwars) {
            if (patternIndex < pattern.size && swar == pattern[patternIndex]) {
                patternIndex++
            }
        }
        return patternIndex == pattern.size
    }
    
    /**
     * Checks for special movement patterns
     */
    private fun checkSpecialPattern(detectedSwars: List<String>, pattern: List<String>): Boolean {
        // For now, just check if the pattern exists as a subsequence
        var patternIndex = 0
        for (swar in detectedSwars) {
            if (patternIndex < pattern.size && swar == pattern[patternIndex]) {
                patternIndex++
            }
        }
        return patternIndex == pattern.size
    }
    
    /**
     * Calculates overall compliance score
     */
    private fun calculateOverallCompliance(
        notePresence: Float,
        forbiddenTransitions: List<String>,
        characteristicPhrases: Float,
        noteHierarchy: Float,
        movementPatterns: Float
    ): Float {
        // Weighted average with different weights for each aspect
        val weights = listOf(
            0.3f, // Note presence
            0.2f, // Forbidden transitions (penalty)
            0.2f, // Characteristic phrases
            0.15f, // Note hierarchy
            0.15f  // Movement patterns
        )
        
        // Calculate base score
        var baseScore = (
            notePresence * weights[0] +
            (if (forbiddenTransitions.isEmpty()) 1f else max(0f, 1f - forbiddenTransitions.size * 0.1f)) * weights[1] +
            characteristicPhrases * weights[2] +
            noteHierarchy * weights[3] +
            movementPatterns * weights[4]
        )
        
        // Ensure score is within bounds
        return baseScore.coerceIn(0f, 1f)
    }
    
    /**
     * Generates suggestions for improving raga compliance
     */
    private fun generateSuggestions(
        detectedSwars: List<String>,
        ragaDefinition: RagaDefinition,
        forbiddenTransitions: List<String>,
        characteristicPhrases: Float
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Suggest practicing characteristic phrases if not detected
        if (characteristicPhrases < 0.3f) {
            suggestions.add("Try practicing the characteristic phrases (pakad) of this raga: ${ragaDefinition.characteristicPhrases.firstOrNull()?.joinToString(" → ") ?: "Not specified"}")
        }
        
        // Suggest avoiding forbidden transitions
        if (forbiddenTransitions.isNotEmpty()) {
            suggestions.addAll(forbiddenTransitions.take(3)) // Limit to 3 suggestions
        }
        
        // Suggest emphasizing important notes
        val importantNotes = ragaDefinition.noteImportance.filter { it.value > 0.6f }.keys
        val detectedImportantNotes = detectedSwars.intersect(importantNotes.toSet())
        if (detectedImportantNotes.size < importantNotes.size * 0.7f) {
            val missingNotes = importantNotes.subtract(detectedImportantNotes)
            suggestions.add("Try emphasizing the important notes of this raga: ${missingNotes.joinToString(", ")}")
        }
        
        return suggestions
    }
    
    /**
     * Gets raga definition with rules and characteristics
     */
    private fun getRagaDefinition(raga: String): RagaDefinition {
        return when (raga) {
            "Yaman" -> RagaDefinition(
                allowedNotes = listOf("Sa", "Re", "Ga", "Ma(t)", "Pa", "Dha", "Ni"),
                forbiddenTransitions = listOf(Pair("Ni", "Sa"), Pair("Dha", "Ga")), // Avoid certain transitions
                characteristicPhrases = listOf(
                    listOf("Ni", "Sa", "Re", "Ga", "Ma(t)", "Pa"),
                    listOf("Pa", "Dha", "Ni", "Sa'")
                ),
                noteImportance = mapOf("Sa" to 0.9f, "Pa" to 0.8f, "Ga" to 0.7f, "Ni" to 0.6f),
                movementPatterns = listOf(
                    MovementPattern(MovementPatternType.AROHA, listOf("Sa", "Re", "Ga", "Ma(t)", "Pa", "Dha", "Ni")),
                    MovementPattern(MovementPatternType.AVAROHA, listOf("Sa'", "Ni", "Dha", "Pa", "Ma(t)", "Ga", "Re", "Sa"))
                )
            )
            "Bhairav" -> RagaDefinition(
                allowedNotes = listOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha(k)", "Ni"),
                forbiddenTransitions = listOf(Pair("Re(k)", "Ga"), Pair("Dha(k)", "Ni")), // Avoid certain transitions
                characteristicPhrases = listOf(
                    listOf("Sa", "Re(k)", "Sa", "Ga", "Ma", "Pa"),
                    listOf("Pa", "Dha(k)", "Pa", "Ma", "Ga", "Re(k)", "Sa")
                ),
                noteImportance = mapOf("Sa" to 0.9f, "Pa" to 0.8f, "Re(k)" to 0.7f, "Dha(k)" to 0.6f),
                movementPatterns = listOf(
                    MovementPattern(MovementPatternType.AROHA, listOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha(k)", "Ni")),
                    MovementPattern(MovementPatternType.AVAROHA, listOf("Sa'", "Ni", "Dha(k)", "Pa", "Ma", "Ga", "Re(k)", "Sa"))
                )
            )
            "Todi" -> RagaDefinition(
                allowedNotes = listOf("Sa", "Re(k)", "Ga(k)", "Ma(t)", "Pa", "Dha(k)", "Ni"),
                forbiddenTransitions = listOf(Pair("Ga(k)", "Dha(k)")), // Avoid certain transitions
                characteristicPhrases = listOf(
                    listOf("Ga(k)", "Ma(t)", "Dha(k)", "Pa"),
                    listOf("Pa", "Ga(k)", "Re(k)", "Sa")
                ),
                noteImportance = mapOf("Sa" to 0.9f, "Pa" to 0.8f, "Ga(k)" to 0.7f, "Ma(t)" to 0.6f),
                movementPatterns = listOf(
                    MovementPattern(MovementPatternType.AROHA, listOf("Sa", "Re(k)", "Ga(k)", "Ma(t)", "Pa", "Dha(k)", "Ni")),
                    MovementPattern(MovementPatternType.AVAROHA, listOf("Sa'", "Ni", "Dha(k)", "Pa", "Ma(t)", "Ga(k)", "Re(k)", "Sa"))
                )
            )
            "Malkauns" -> RagaDefinition(
                allowedNotes = listOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)"),
                forbiddenTransitions = listOf(Pair("Ni(k)", "Ga(k)"), Pair("Dha(k)", "Sa")), // Avoid certain transitions
                characteristicPhrases = listOf(
                    listOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)", "Sa'"),
                    listOf("Ni(k)", "Dha(k)", "Ga(k)", "Ma", "Sa")
                ),
                noteImportance = mapOf("Sa" to 0.9f, "Ma" to 0.8f, "Ga(k)" to 0.7f, "Dha(k)" to 0.6f),
                movementPatterns = listOf(
                    MovementPattern(MovementPatternType.AROHA, listOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)")),
                    MovementPattern(MovementPatternType.AVAROHA, listOf("Sa'", "Ni(k)", "Dha(k)", "Ma", "Ga(k)", "Sa"))
                )
            )
            "Bhupali" -> RagaDefinition(
                allowedNotes = listOf("Sa", "Re", "Ga", "Pa", "Dha"),
                forbiddenTransitions = listOf(Pair("Ga", "Re"), Pair("Dha", "Pa")), // Avoid certain transitions
                characteristicPhrases = listOf(
                    listOf("Sa", "Re", "Ga", "Pa", "Dha", "Sa'"),
                    listOf("Dha", "Pa", "Ga", "Re", "Sa")
                ),
                noteImportance = mapOf("Sa" to 0.9f, "Pa" to 0.8f, "Re" to 0.7f, "Dha" to 0.6f),
                movementPatterns = listOf(
                    MovementPattern(MovementPatternType.AROHA, listOf("Sa", "Re", "Ga", "Pa", "Dha")),
                    MovementPattern(MovementPatternType.AVAROHA, listOf("Sa'", "Dha", "Pa", "Ga", "Re", "Sa"))
                )
            )
            else -> RagaDefinition(
                allowedNotes = listOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni"),
                forbiddenTransitions = emptyList(),
                characteristicPhrases = emptyList(),
                noteImportance = mapOf("Sa" to 0.9f, "Pa" to 0.8f),
                movementPatterns = emptyList()
            )
}
}
}
