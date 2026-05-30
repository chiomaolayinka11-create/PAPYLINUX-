package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TermuxTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val command: String,
    val workDir: String? = null,
    val isBackground: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Pending", // "Pending", "Running", "Success", "Error"
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int? = null,
    val errCode: Int? = null,
    val errMsg: String = "",
    val isAiGenerated: Boolean = false
)
