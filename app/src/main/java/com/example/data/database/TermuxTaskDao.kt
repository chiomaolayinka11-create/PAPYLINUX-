package com.example.data.database

import androidx.room.*
import com.example.data.model.TermuxTask
import kotlinx.coroutines.flow.Flow

@Dao
interface TermuxTaskDao {
    @Query("SELECT * FROM tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<TermuxTask>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): TermuxTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TermuxTask): Long

    @Update
    suspend fun updateTask(task: TermuxTask)

    @Delete
    suspend fun deleteTask(task: TermuxTask)

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()
}
