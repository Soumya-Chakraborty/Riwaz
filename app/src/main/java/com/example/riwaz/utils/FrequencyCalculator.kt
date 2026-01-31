package com.example.riwaz.utils

/**
 * Utility to calculate expected frequencies for Hindustani Classical Swars
 * based on a western base frequency (Scale).
 */
object FrequencyCalculator {
    private val baseFrequencies = mapOf(
        "C" to 261.63f,
        "D" to 293.66f,
        "E" to 329.63f,
        "F" to 349.23f,
        "G" to 392.00f,
        "A" to 440.00f,
        "B" to 493.88f
    )

    private val swarRatios = mapOf(
        "Sa" to 1.0f,
        "Re(k)" to 1.053f,
        "Re" to 1.125f,
        "Ga(k)" to 1.189f,
        "Ga" to 1.260f,
        "Ma" to 1.335f,
        "Ma(t)" to 1.414f,
        "Pa" to 1.498f,
        "Dha(k)" to 1.587f,
        "Dha" to 1.682f,
        "Ni(k)" to 1.782f,
        "Ni" to 1.888f
    )

    fun getBaseFrequency(scale: String): Float =
        baseFrequencies[scale] ?: baseFrequencies["C"]!!

    fun calculate(swar: String, scale: String): Float {
        val base = getBaseFrequency(scale)
        val ratio = swarRatios[swar] ?: 1.0f
        return base * ratio
    }
}