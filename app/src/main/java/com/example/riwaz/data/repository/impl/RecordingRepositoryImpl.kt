package com.example.riwaz.data.repository.impl

import android.content.Context
import android.util.Log
import com.example.riwaz.data.repository.RecordingRepository
import com.example.riwaz.models.PracticeSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class RecordingRepositoryImpl(private val context: Context) : RecordingRepository {
    override fun getRecordings(): Flow<List<PracticeSession>> = flow {
        // In a real implementation, this would fetch from Room database
        val files = getRecordingFiles()
        val sessions = files.mapNotNull { file ->
            loadMetadata(file)?.let { metadata ->
                val notes = loadNotesFromMetadata(file)
                PracticeSession(
                    file = file,
                    raga = metadata.first,
                    practiceType = metadata.second,
                    tempo = metadata.third,
                    notes = notes
                )
            }
        }.sortedByDescending { it.file.lastModified() }
        
        emit(sessions)
    }

    override suspend fun saveRecording(session: PracticeSession) {
        // Save the recording and metadata
        saveMetadata(session)

        // Ensure the file exists and is properly indexed
        if (!session.file.exists()) {
            Log.e("RecordingRepositoryImpl", "Recording file does not exist: ${session.file.absolutePath}")
        }
    }

    override suspend fun deleteRecording(session: PracticeSession) {
        session.file.delete()
        File(session.file.absolutePath.replace(".m4a", ".meta")).delete()
    }

    override suspend fun getRecordingById(id: String): PracticeSession? {
        // In a real implementation, this would query the database
        return null
    }

    private fun getRecordingFiles(): Array<File> {
        // Access the application's files directory using the context
        val filesDir = context.filesDir
        return filesDir.listFiles { file -> file.extension == "m4a" } ?: arrayOf()
    }

    private fun loadMetadata(file: File): Triple<String, String, String>? {
        // Load metadata from .meta file
        val metadataFile = File(file.absolutePath.replace(".m4a", ".meta"))
        return if (metadataFile.exists()) {
            val parts = metadataFile.readText().split("|")
            Triple(
                parts.getOrNull(0) ?: "Unknown Raga",
                parts.getOrNull(1) ?: "Practice",
                parts.getOrNull(2) ?: "Madhya"
            )
        } else {
            null
        }
    }

    private fun loadNotesFromMetadata(file: File): String {
        // Load notes from .meta file
        val metadataFile = File(file.absolutePath.replace(".m4a", ".meta"))
        return if (metadataFile.exists()) {
            val parts = metadataFile.readText().split("|")
            parts.getOrNull(3) ?: ""
        } else {
            ""
        }
    }

    private fun saveMetadata(session: PracticeSession) {
        val metadataFile = File(session.file.absolutePath.replace(".m4a", ".meta"))
        try {
            val safeNotes = session.notes.replace("|", " ")
            metadataFile.writeText("${session.raga}|${session.practiceType}|${session.tempo}|$safeNotes")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}