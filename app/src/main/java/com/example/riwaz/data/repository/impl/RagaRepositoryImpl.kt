package com.example.riwaz.data.repository.impl

import com.example.riwaz.data.repository.RagaRepository
import com.example.riwaz.ui.components.RagaRegistry

class RagaRepositoryImpl : RagaRepository {
    override fun getRagaData(ragaName: String): RagaRegistry.RagaData {
        return RagaRegistry.getRagaData(ragaName)
    }

    override fun getAllRagas(): List<String> {
        return RagaRegistry::class.java.getDeclaredField("ragaDefinitions")
            .apply { isAccessible = true }
            .get(RagaRegistry).let { 
                @Suppress("UNCHECKED_CAST")
                (it as Map<String, *>).keys.toList()
            }
    }

    override fun getRagasByTimeOfDay(timeOfDay: String): List<String> {
        // This would normally be implemented based on raga time classifications
        return when (timeOfDay.lowercase()) {
            "morning" -> listOf("Bhairav", "Todi", "Lalit", "Ahir Bhairav", "Ramkali")
            "afternoon" -> listOf("Sarang", "Madhyamad Sarang", "Multani", "Patdeep")
            "evening" -> listOf("Yaman", "Bihag", "Puriya", "Shree", "Marwa")
            "night" -> listOf("Malkauns", "Darbari", "Bageshri", "Jaunpuri", "Kedar")
            else -> listOf("Bhupali", "Deshkar", "Kafi", "Khamaj", "Des")
        }
    }
}