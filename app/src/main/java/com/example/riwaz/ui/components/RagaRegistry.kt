package com.example.riwaz.ui.components

import com.example.riwaz.utils.AudioProcessor
import com.example.riwaz.utils.ErrorCategory
import com.example.riwaz.utils.ErrorDetail
import com.example.riwaz.utils.ErrorSeverity
import com.example.riwaz.utils.SwarData
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Centralized registry for Raga definitions with real analysis capabilities.
 */
object RagaRegistry {
    data class RagaData(
        val name: String,
        val swars: List<String>,
        val tips: List<String>,
        val vadi: String? = null,
        val samvadi: String? = null
    )

    private val ragaDefinitions = mapOf(
        "Bhairav" to RagaData(
            "Bhairav",
            listOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha(k)", "Ni"),
            listOf(
                "Emphasize Komal Re and Dha",
                "Add gentle andolan to Re",
                "Practice in early morning hours"
            ),
            vadi = "Dha(k)",
            samvadi = "Re(k)"
        ),
        "Todi" to RagaData(
            "Todi",
            listOf("Sa", "Re(k)", "Ga(k)", "Ma(t)", "Pa", "Dha(k)", "Ni"),
            listOf(
                "Komal Re, Ga, Dha and Teevra Ma create its unique mood",
                "Pa is often used as a resting note"
            ),
            vadi = "Dha(k)",
            samvadi = "Ga(k)"
        ),
        "Lalit" to RagaData(
            "Lalit",
            listOf("Sa", "Re", "Ga", "Ma", "Ma(t)", "Dha(k)", "Ni"),
            listOf(
                "The transition between two Mas is characteristic",
                "Avoid Pa entirely"
            ),
            vadi = "Ma",
            samvadi = "Sa"
        ),
        "Ahir Bhairav" to RagaData(
            "Ahir Bhairav",
            listOf("Sa", "Re(k)", "Ga", "Ma", "Pa", "Dha", "Ni(k)"),
            listOf(
                "Focus on the Ni(k) to Sa transition",
                "The Re(k) should be very soft"
            ),
            vadi = "Ma",
            samvadi = "Sa"
        ),
        "Yaman" to RagaData(
            "Yaman",
            listOf("Sa", "Re", "Ga", "Ma(t)", "Pa", "Dha", "Ni"),
            listOf(
                "Teevra Ma is the life of Yaman",
                "Ni to Re meend is crucial"
            ),
            vadi = "Ga",
            samvadi = "Ni"
        ),
        "Bhupali" to RagaData(
            "Bhupali",
            listOf("Sa", "Re", "Ga", "Pa", "Dha"),
            listOf(
                "Pentatonic scale, avoid Ma and Ni",
                "Keep the notes pure and shuddh"
            ),
            vadi = "Ga",
            samvadi = "Dha"
        ),
        "Malkauns" to RagaData(
            "Malkauns",
            listOf("Sa", "Ga(k)", "Ma", "Dha(k)", "Ni(k)"),
            listOf(
                "Omit Re and Pa",
                "Focus on heavy, deep oscillations"
            ),
            vadi = "Ma",
            samvadi = "Sa"
        ),
        "Darbari" to RagaData(
            "Darbari",
            listOf("Sa", "Re", "Ga(k)", "Ma", "Pa", "Dha(k)", "Ni(k)"),
            listOf("Slow, heavy andolan on Ga(k) and Dha(k)"),
            vadi = "Re",
            samvadi = "Pa"
        ),
        "Kafi" to RagaData(
            "Kafi",
            listOf("Sa", "Re", "Ga(k)", "Ma", "Pa", "Dha", "Ni(k)"),
            listOf("Foundational raga for many folk tunes"),
            vadi = "Pa",
            samvadi = "Sa"
        )
    )

    fun getRagaData(raga: String): RagaData =
        ragaDefinitions[raga] ?: RagaData(
            raga,
            listOf("Sa", "Re", "Ga", "Ma", "Pa", "Dha", "Ni"),
            listOf("Focus on pitch accuracy")
        )

    /**
     * Retrieves real error analysis for a recording with scale consideration.
     */
    suspend fun getErrorsForRecording(recordingFile: File, raga: String, scale: String = "C (261.63 Hz)"): List<ErrorDetail> {
        val audioProcessor = AudioProcessor()
        val basicErrors = audioProcessor.analyzeErrors(recordingFile, raga, scale)

        // Add raga grammar validation
        val grammarErrors = validateRagaGrammar(recordingFile, raga, scale)

        // Combine basic errors with grammar errors
        return basicErrors + grammarErrors
    }

    /**
     * Validates the recording against raga-specific grammar rules with scale consideration
     */
    suspend fun validateRagaGrammar(recordingFile: File, raga: String, scale: String = "C (261.63 Hz)"): List<ErrorDetail> {
        val audioProcessor = AudioProcessor()
        val swarSequence = audioProcessor.getDetectedSwarSequence(recordingFile, raga, scale)

        val grammarErrors = mutableListOf<ErrorDetail>()
        val ragaGrammar = getRagaGrammar(raga)

        // Check for forbidden note transitions
        for (i in 1 until swarSequence.size) {
            val prevSwar = swarSequence[i-1]
            val currentSwar = swarSequence[i]

            if (ragaGrammar.forbiddenTransitions.contains(Pair(prevSwar, currentSwar))) {
                grammarErrors.add(
                    ErrorDetail(
                        category = ErrorCategory.PITCH,
                        swar = "$prevSwar->$currentSwar",
                        severity = ErrorSeverity.MAJOR,
                        description = "Forbidden transition in $raga: $prevSwar to $currentSwar",
                        correction = "Follow proper note progression in $raga, avoid moving directly from $prevSwar to $currentSwar"
                    )
                )
            }
        }

        // Check for missing characteristic phrases
        val characteristicPhrases = ragaGrammar.characteristicPhrases
        val foundPhrases = mutableListOf<List<String>>()

        for (phrase in characteristicPhrases) {
            if (containsPhrase(swarSequence, phrase)) {
                foundPhrases.add(phrase)
            }
        }

        // If no characteristic phrases were used, add a suggestion
        if (foundPhrases.isEmpty() && characteristicPhrases.isNotEmpty()) {
            grammarErrors.add(
                ErrorDetail(
                    category = ErrorCategory.EXPRESSION,
                    swar = "Characteristic Phrases",
                    severity = ErrorSeverity.MINOR,
                    description = "No characteristic phrases of $raga were detected",
                    correction = "Include characteristic phrases of $raga to enhance raga identity"
                )
            )
        }

        return grammarErrors
    }

    /**
     * Checks if a sequence contains a specific phrase
     */
    private fun containsPhrase(sequence: List<String>, phrase: List<String>): Boolean {
        if (phrase.size > sequence.size) return false

        for (i in 0..(sequence.size - phrase.size)) {
            val subSequence = sequence.subList(i, i + phrase.size)
            if (subSequence == phrase) return true
        }

        return false
    }

    /**
     * Gets raga-specific grammar rules
     */
    private fun getRagaGrammar(raga: String): RagaGrammar {
        return when (raga) {
            "Yaman" -> RagaGrammar(
                allowedTransitions = mapOf(
                    "Sa" to listOf("Re", "Ga", "Pa"),
                    "Re" to listOf("Ga", "Ma", "Pa"),
                    "Ga" to listOf("Ma", "Pa"),
                    "Ma" to listOf("Pa", "Dha", "Ma(t)"),
                    "Ma(t)" to listOf("Pa"),
                    "Pa" to listOf("Dha", "Ni", "Sa'"),
                    "Dha" to listOf("Ni", "Pa", "Ga"),
                    "Ni" to listOf("Sa'", "Dha", "Pa")
                ),
                forbiddenTransitions = listOf(
                    Pair("Ni", "Re"), // Avoid Ni to Re direct jump in Yaman
                    Pair("Dha", "Re")  // Avoid Dha to Re direct jump
                ),
                characteristicPhrases = listOf(
                    listOf("Ma(t)", "Pa", "Ni", "Sa'"), // Yaman's characteristic phrase
                    listOf("Ni", "Dha", "Pa", "Ma(t)")  // Another common phrase
                )
            )
            "Bhairav" -> RagaGrammar(
                allowedTransitions = mapOf(
                    "Sa" to listOf("Re(k)", "Ga", "Ma"),
                    "Re(k)" to listOf("Ga", "Ma"),
                    "Ga" to listOf("Ma", "Pa"),
                    "Ma" to listOf("Pa", "Dha(k)"),
                    "Pa" to listOf("Dha(k)", "Ni", "Sa'"),
                    "Dha(k)" to listOf("Ni", "Pa", "Ga"),
                    "Ni" to listOf("Sa'", "Dha(k)", "Pa")
                ),
                forbiddenTransitions = listOf(
                    Pair("Re(k)", "Dha(k)"), // Avoid direct jump between komal notes
                    Pair("Ga", "Ni")         // Avoid Ga to Ni direct jump
                ),
                characteristicPhrases = listOf(
                    listOf("Sa", "Re(k)", "Ga", "Ma"), // Morning raga phrase
                    listOf("Pa", "Dha(k)", "Ni", "Sa'") // Evening phrase
                )
            )
            else -> RagaGrammar(
                allowedTransitions = mapOf(
                    "Sa" to listOf("Re", "Ga", "Pa"),
                    "Re" to listOf("Ga", "Ma", "Pa"),
                    "Ga" to listOf("Ma", "Pa"),
                    "Ma" to listOf("Pa", "Dha"),
                    "Pa" to listOf("Dha", "Ni", "Sa'"),
                    "Dha" to listOf("Ni", "Pa"),
                    "Ni" to listOf("Sa'", "Pa")
                ),
                forbiddenTransitions = emptyList(),
                characteristicPhrases = emptyList()
            )
        }
    }

    /**
     * Data class for raga grammar rules
     */
    data class RagaGrammar(
        val allowedTransitions: Map<String, List<String>>,
        val forbiddenTransitions: List<Pair<String, String>>,
        val characteristicPhrases: List<List<String>>
    )

    /**
     * Retrieves real swar statistics for a recording with scale consideration.
     */
    suspend fun getSwarStatsForRecording(
        recordingFile: File,
        raga: String,
        scale: String = "C (261.63 Hz)",
        accuracyThreshold: Float = 0.7f
    ): List<SwarData> {
        val audioProcessor = AudioProcessor()
        return audioProcessor.analyzeRecording(recordingFile, raga, scale, accuracyThreshold)
    }

    /**
     * Calculates real vibrato score for a recording with scale consideration.
     */
    suspend fun getVibratoScoreForRecording(recordingFile: File, scale: String = "C (261.63 Hz)"): Float {
        val audioProcessor = AudioProcessor()
        return audioProcessor.analyzeVibrato(recordingFile, scale)
    }

    /**
     * Calculates real overall accuracy for a recording with scale consideration.
     */
    suspend fun getOverallAccuracyForRecording(recordingFile: File, raga: String, scale: String = "C (261.63 Hz)"): Float {
        val swarStats = getSwarStatsForRecording(recordingFile, raga, scale)
        val audioProcessor = AudioProcessor()
        return audioProcessor.calculateOverallAccuracy(swarStats)
    }

    /**
     * Calculates real average stability for a recording with scale consideration.
     */
    suspend fun getAverageStabilityForRecording(recordingFile: File, raga: String, scale: String = "C (261.63 Hz)"): Float {
        val swarStats = getSwarStatsForRecording(recordingFile, raga, scale)
        val audioProcessor = AudioProcessor()
        return audioProcessor.calculateAverageStability(swarStats)
    }
}
