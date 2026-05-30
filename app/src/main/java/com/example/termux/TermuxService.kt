package com.example.termux

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.TermuxTask

object TermuxService {
    fun execute(context: Context, task: TermuxTask) {
        try {
            val intent = Intent()
            intent.setClassName("com.termux", "com.termux.app.RunCommandService")
            intent.action = "com.termux.RUN_COMMAND"
            
            // Base executable defaults to Termux bash
            val executablePath = task.workDir ?: "/data/data/com.termux/files/usr/bin/bash"
            intent.putExtra("com.termux.RUN_COMMAND_PATH", executablePath)
            
            // Build arguments array
            // If running a bin directly that is not sh/bash, pass it as arguments,
            // otherwise execute the user's script line via bash -c "..."
            val args = if (executablePath.endsWith("bash") || executablePath.endsWith("sh")) {
                arrayOf("-c", task.command)
            } else {
                arrayOf(task.command)
            }
            intent.putExtra("com.termux.RUN_COMMAND_ARGUMENTS", args)
            
            // Set working directory
            intent.putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            
            // Background vs Terminal Session
            intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", task.isBackground)
            intent.putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            
            // PendingIntent for output results callback
            val callbackIntent = Intent(context, TermuxResultReceiver::class.java).apply {
                action = "com.example.termux.RESULT_CALLBACK"
                putExtra("request_id", task.id)
            }
            
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                task.id,
                callbackIntent,
                pendingIntentFlags
            )
            intent.putExtra("com.termux.RUN_COMMAND_PENDING_INTENT", pendingIntent)
            
            // Start Service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startService(intent)
                } catch (e: Exception) {
                    Log.w("TermuxService", "Failed typical background startService call, trying foreground fallback", e)
                    try {
                        context.startForegroundService(intent)
                    } catch (fe: Exception) {
                        Log.e("TermuxService", "Failed both startService and startForegroundService for Termux", fe)
                    }
                }
            } else {
                try {
                    context.startService(intent)
                } catch (e: Exception) {
                    Log.e("TermuxService", "Failed starting service on legacy SDK version", e)
                }
            }
            
            Log.d("TermuxService", "Dispatched task ${task.id} command to Termux successfully.")
        } catch (e: Throwable) {
            Log.e("TermuxService", "Error executing Termux command", e)
        }
    }
}
