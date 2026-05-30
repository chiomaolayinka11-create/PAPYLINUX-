package com.example.termux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        val stdout = intent.getStringExtra("stdout") ?: intent.getStringExtra("stdout_original") ?: ""
        val stderr = intent.getStringExtra("stderr") ?: ""
        val exitCode = intent.getIntExtra("exitCode", -1)
        val errCode = intent.getIntExtra("errCode", -1)
        val errMsg = intent.getStringExtra("errmsg") ?: ""
        val requestId = intent.getIntExtra("request_id", -1)
        
        Log.d("TermuxResultReceiver", "Received callback: requestId=$requestId, exitCode=$exitCode, errCode=$errCode, errMsg=$errMsg")
        
        if (requestId != -1) {
            val database = AppDatabase.getDatabase(context)
            val taskDao = database.taskDao()
            
            CoroutineScope(Dispatchers.IO).launch {
                val existingTask = taskDao.getTaskById(requestId)
                if (existingTask != null) {
                    val finalStatus = if (errCode != -1 && errCode != 0) {
                        "Error"
                    } else if (exitCode != -1 && exitCode != 0) {
                        "Error"
                    } else {
                        "Success"
                    }
                    
                    val updatedTask = existingTask.copy(
                        status = finalStatus,
                        stdout = stdout,
                        stderr = stderr,
                        exitCode = if (exitCode != -1) exitCode else null,
                        errCode = if (errCode != -1) errCode else null,
                        errMsg = errMsg
                    )
                    taskDao.updateTask(updatedTask)
                }
            }
        }
    }
}
