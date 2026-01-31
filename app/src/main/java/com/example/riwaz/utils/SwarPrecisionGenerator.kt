package com.example.riwaz.utils

import com.example.riwaz.ui.components.RagaRegistry
import com.example.riwaz.utils.SwarData
import kotlin.random.Random

/**
 * Utility class for generating sample swar precision data for testing and demonstration
 */
object SwarPrecisionGenerator {
    
    /**
     * Generates sample swar data for visualization
     */
    fun generateSampleSwarData(ragaName: String = "Yaman", count: Int = 50): List<SwarData> {
        val ragaInfo = RagaRegistry.getRagaData(ragaName)
        val swars = ragaInfo.swars
        val sampleData = mutableListOf<SwarData>()
        
        repeat(count) { index ->
            val swarName = swars.random()
            val accuracy = Random.nextFloat() // Random accuracy between 0 and 1
            val stability = Random.nextFloat() // Random stability between 0 and 1
            val expectedFreq = FrequencyCalculator.calculate(swarName, "C")
            val detectedFreq = expectedFreq + (Random.nextFloat() * 20 - 10) // Add some deviation
            
            sampleData.add(
                SwarData(
                    name = swarName,
                    accuracy = accuracy,
                    isMistake = accuracy < 0.7f,
                    expectedFreq = expectedFreq,
                    detectedFreq = detectedFreq,
                    stability = stability
                )
            )
        }
        
        return sampleData
    }
    
    /**
     * Generates sample timeline data with accuracy fluctuations over time
     */
    fun generateTimelineSwarData(ragaName: String = "Yaman", count: Int = 100): List<SwarData> {
        val ragaInfo = RagaRegistry.getRagaData(ragaName)
        val swars = ragaInfo.swars
        val sampleData = mutableListOf<SwarData>()
        
        repeat(count) { index ->
            val swarName = swars.random()
            val expectedFreq = FrequencyCalculator.calculate(swarName, "C")
            
            // Create a pattern where accuracy improves over time with some fluctuations
            val baseAccuracy = minOf(1.0f, 0.3f + (index.toFloat() / count.toFloat()) * 0.7f)
            val fluctuation = (kotlin.math.sin(index.toDouble() * 0.3) * 0.1).toFloat()
            val accuracy = maxOf(0.0f, minOf(1.0f, baseAccuracy + fluctuation + Random.nextFloat() * 0.1f - 0.05f))
            
            val stability = maxOf(0.0f, minOf(1.0f, 0.6f + Random.nextFloat() * 0.4f - 0.1f))
            val detectedFreq = expectedFreq + (Random.nextFloat() * 30 - 15) // Add some deviation
            
            sampleData.add(
                SwarData(
                    name = swarName,
                    accuracy = accuracy,
                    isMistake = accuracy < 0.7f,
                    expectedFreq = expectedFreq,
                    detectedFreq = detectedFreq,
                    stability = stability
                )
            )
        }
        
        return sampleData
    }
}