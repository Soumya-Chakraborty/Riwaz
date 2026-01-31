package com.example.riwaz.models

import java.io.File

data class RagaCategory(
    val name: String,
    val ragas: List<String>
)

data class PracticeSession(
    val file: File,
    val raga: String,
    val practiceType: String,
    val tempo: String,
    val notes: String = ""
)
