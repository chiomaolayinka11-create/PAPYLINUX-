package com.example.data.repository

import com.example.data.database.TermuxTaskDao
import com.example.data.model.TermuxTask
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TermuxTaskDao) {
    val allTasks: Flow<List<TermuxTask>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: Int): TermuxTask? {
        return taskDao.getTaskById(id)
    }

    suspend fun insert(task: TermuxTask): Int {
        return taskDao.insertTask(task).toInt()
    }

    suspend fun update(task: TermuxTask) {
        taskDao.updateTask(task)
    }

    suspend fun delete(task: TermuxTask) {
        taskDao.deleteTask(task)
    }

    suspend fun clearAll() {
        taskDao.clearAllTasks()
    }
}
