package com.example.riwaz.utils

/**
 * Utility for managing different reference scales (Sa positions) in Indian Classical Music
 * Allows users to select their preferred tonic position for analysis
 */
object ScaleManager {
    
    /**
     * Represents different reference scales with Sa at different frequencies
     */
    data class ReferenceScale(
        val name: String,
        val saFrequency: Float, // Base frequency for Sa (Shadja)
        val description: String
    )
    
    /**
     * Common reference scales used in Indian Classical Music
     * Based on different octaves and tuning preferences
     */
    val REFERENCE_SCALES = listOf(
        ReferenceScale("C (261.63 Hz)", 261.63f, "Standard concert pitch for Sa"),
        ReferenceScale("C# (277.18 Hz)", 277.18f, "Higher semitone reference"),
        ReferenceScale("D (293.66 Hz)", 293.66f, "Higher octave reference"),
        ReferenceScale("D# (311.13 Hz)", 311.13f, "Higher semitone reference"),
        ReferenceScale("E (329.63 Hz)", 329.63f, "Higher octave reference"),
        ReferenceScale("F (349.23 Hz)", 349.23f, "Alternative reference"),
        ReferenceScale("F# (369.99 Hz)", 369.99f, "Alternative semitone reference"),
        ReferenceScale("G (392.00 Hz)", 392.00f, "Common reference for higher voices"),
        ReferenceScale("G# (415.30 Hz)", 415.30f, "Higher semitone reference"),
        ReferenceScale("A (440.00 Hz)", 440.00f, "Standard tuning reference"),
        ReferenceScale("A# (466.16 Hz)", 466.16f, "Higher semitone reference"),
        ReferenceScale("B (493.88 Hz)", 493.88f, "Higher octave reference")
    )
    
    /**
     * Gets the default reference scale (C)
     */
    fun getDefaultScale(): ReferenceScale = REFERENCE_SCALES[0]
    
    /**
     * Gets a reference scale by name
     */
    fun getScaleByName(name: String): ReferenceScale? {
        return REFERENCE_SCALES.find { it.name == name }
    }
    
    /**
     * Calculates the frequency of a swar in a given scale using traditional Indian classical ratios
     */
    fun calculateSwarFrequency(swar: String, scale: ReferenceScale): Float {
        val baseFreq = scale.saFrequency
        val ratio = when (swar) {
            "Sa" -> 1.0f                           // Sa (Shadja) - Fundamental
            "Re(k)" -> 16.0f/15.0f                // Komal Re (16/15 - 1.067)
            "Re" -> 9.0f/8.0f                     // Shuddha Re (9/8 - 1.125)
            "Ga(k)" -> 6.0f/5.0f                  // Komal Ga (6/5 - 1.200)
            "Ga" -> 5.0f/4.0f                     // Shuddha Ga (5/4 - 1.250)
            "Ma" -> 4.0f/3.0f                     // Shuddha Ma (4/3 - 1.333)
            "Ma(t)" -> 7.0f/5.0f                  // Tivra Ma (7/5 - 1.400)
            "Pa" -> 3.0f/2.0f                     // Pa (Panchama) (3/2 - 1.500)
            "Dha(k)" -> 8.0f/5.0f                 // Komal Dha (8/5 - 1.600)
            "Dha" -> 5.0f/3.0f                    // Shuddha Dha (5/3 - 1.667)
            "Ni(k)" -> 9.0f/5.0f                  // Komal Ni (9/5 - 1.800)
            "Ni" -> 15.0f/8.0f                    // Shuddha Ni (15/8 - 1.875)
            else -> 1.0f                           // Default to Sa
        }

        return baseFreq * ratio
    }
    
    /**
     * Gets all swar frequencies for a given scale
     */
    fun getAllSwarFrequencies(scale: ReferenceScale): Map<String, Float> {
        return mapOf(
            "Sa" to calculateSwarFrequency("Sa", scale),
            "Re(k)" to calculateSwarFrequency("Re(k)", scale),
            "Re" to calculateSwarFrequency("Re", scale),
            "Ga(k)" to calculateSwarFrequency("Ga(k)", scale),
            "Ga" to calculateSwarFrequency("Ga", scale),
            "Ma" to calculateSwarFrequency("Ma", scale),
            "Ma(t)" to calculateSwarFrequency("Ma(t)", scale),
            "Pa" to calculateSwarFrequency("Pa", scale),
            "Dha(k)" to calculateSwarFrequency("Dha(k)", scale),
            "Dha" to calculateSwarFrequency("Dha", scale),
            "Ni(k)" to calculateSwarFrequency("Ni(k)", scale),
            "Ni" to calculateSwarFrequency("Ni", scale)
        )
    }
}