package com.example.riwaz.data.repository

import com.example.riwaz.ui.components.RagaRegistry

interface RagaRepository {
    fun getRagaData(ragaName: String): RagaRegistry.RagaData
    fun getAllRagas(): List<String>
    fun getRagasByTimeOfDay(timeOfDay: String): List<String>
}