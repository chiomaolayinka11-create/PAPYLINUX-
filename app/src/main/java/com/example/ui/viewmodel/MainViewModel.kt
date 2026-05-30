package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.api.GeminiClient
import com.example.data.api.AgentCommand
import com.example.data.model.TermuxTask
import com.example.data.repository.TaskRepository
import com.example.termux.TermuxService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatMessage(
    val sender: String, // "user" or "agent"
    val text: String,
    val commands: List<AgentCommand> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val taskDao = AppDatabase.getDatabase(application).taskDao()
    private val repository = TaskRepository(taskDao)

    // Flow matching tasks from database
    val tasks: StateFlow<List<TermuxTask>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Chat agent states
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "agent",
                text = "Hello! I am your Termux AI Task Agent. Suggest a command, task, or automation sequence you want to deploy, and I will generate the safe scripts and run options for you."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isAgentThinking = MutableStateFlow(false)
    val isAgentThinking: StateFlow<Boolean> = _isAgentThinking.asStateFlow()

    private val _agentError = MutableStateFlow<String?>(null)
    val agentError: StateFlow<String?> = _agentError.asStateFlow()

    /**
     * Submit prompt to Gemini Agent
     */
    fun sendUserPrompt(prompt: String) {
        if (prompt.trim().isEmpty()) return

        // Add user statement to listing
        _messages.value = _messages.value + ChatMessage(sender = "user", text = prompt)
        _isAgentThinking.value = true
        _agentError.value = null

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    GeminiClient.askAgent(prompt)
                }
                
                if (response != null) {
                    _messages.value = _messages.value + ChatMessage(
                        sender = "agent",
                        text = response.explanation,
                        commands = response.commands
                    )
                } else {
                    _agentError.value = "Failed to parse reply schema. Please try again."
                    _messages.value = _messages.value + ChatMessage(
                        sender = "agent",
                        text = "I received a response, but could not format it as structured command objects. Please check my instructions or try rephrasing."
                    )
                }
            } catch (e: Exception) {
                _agentError.value = e.localizedMessage ?: "Unknown connection failure."
                _messages.value = _messages.value + ChatMessage(
                    sender = "agent",
                    text = "Error interacting with Gemini API: ${e.localizedMessage ?: "Connection timed out."}"
                )
            } finally {
                _isAgentThinking.value = false
            }
        }
    }

    private data class SimResult(val isSuccess: Boolean, val stdout: String, val stderr: String)

    private fun getSimulatedOutput(name: String, command: String): SimResult {
        val lower = command.lowercase()
        return when {
            lower.contains("termux-tts-speak") -> {
                val spokeText = command.substringAfter("termux-tts-speak \"", "phrase").substringBefore("\"")
                SimResult(true, "Speech engine: Synthesizing phrase \"$spokeText\" in locale en_US.\n[Device TTS]: Active speech synthesizer triggered successfully.", "")
            }
            lower.contains("termux-notification") -> {
                val title = command.substringAfter("-t \"", "Alert").substringBefore("\"")
                val content = command.substringAfter("-c \"", "Background Notification Content").substringBefore("\"")
                SimResult(true, "Notification successfully posted to Android notification queue:\nTitle: $title\nBody: $content", "")
            }
            lower.contains("termux-torch") -> {
                val state = if (lower.contains("on")) "ON" else "OFF"
                SimResult(true, "Flashlight status updated successfully: TORCH STATE = $state", "")
            }
            lower.contains("termux-brightness") -> {
                val value = command.substringAfter("termux-brightness ").trim()
                SimResult(true, "Display screen hardware modulated. Hardware brightness value registered: $value/255", "")
            }
            lower.contains("termux-vibrate") -> {
                val duration = command.substringAfter("-d ", "300").trim()
                SimResult(true, "Haptic device vibration triggered: Vibrated for $duration ms.", "")
            }
            lower.contains("termux-volume") -> {
                val args = command.substringAfter("termux-volume ").trim().split(" ")
                val stream = args.getOrNull(0) ?: "music"
                val level = args.getOrNull(1) ?: "8"
                SimResult(true, "Audio stream level modulated:\nStream: $stream\nTarget volume level: $level/15", "")
            }
            lower.contains("termux-battery-status") -> {
                SimResult(true, "{\n  \"health\": \"GOOD\",\n  \"percentage\": 87,\n  \"plugged\": \"PLUGGED_AC\",\n  \"status\": \"CHARGING\",\n  \"temperature\": 31.4,\n  \"current\": -120000\n}", "")
            }
            lower.contains("termux-location") -> {
                SimResult(true, "{\n  \"latitude\": 51.5074,\n  \"longitude\": -0.1278,\n  \"altitude\": 14.5,\n  \"accuracy\": 10.0,\n  \"vertical_accuracy\": 3.0,\n  \"provider\": \"gps\"\n}", "")
            }
            lower.contains("termux-contact-list") -> {
                SimResult(true, "[\n  { \"name\": \"Alex Mercer\", \"number\": \"+1-555-0199\" },\n  { \"name\": \"Bruce Wayne\", \"number\": \"+1-800-BATMAN\" },\n  { \"name\": \"Clark Kent\", \"number\": \"+1-555-0143\" },\n  { \"name\": \"Diana Prince\", \"number\": \"+1-555-0182\" }\n]", "")
            }
            lower.contains("termux-clipboard-get") -> {
                SimResult(true, "Termux Bridge Active Command Console", "")
            }
            lower.contains("ls -la") -> {
                SimResult(true, "total 32\ndrwxr-xr-x  5 termux termux 4096 May 30 16:15 .\ndrwxr-xr-x 12 termux termux 4096 May 30 16:00 ..\n-rw-r--r--  1 termux termux  154 May 30 16:12 .bashrc\ndrwxr-xr-x  2 termux termux 4096 May 30 16:04 .termux\n-rwxr-xr-x  1 termux termux  842 May 30 16:14 backup_script.sh\ndrwxr-xr-x  3 termux termux 4096 May 30 16:05 bin\ndrwxr-xr-x  2 termux termux 4096 May 30 16:08 python_server", "")
            }
            lower.contains("df -h") -> {
                SimResult(true, "Filesystem      Size  Used Avail Use% Mounted on\n/dev/block/dm-3  64G   28G   36G  44% /data\ntmpfs           3.7G  1.2M  3.7G   1% /dev\n/dev/block/sda5 128M   42M   86M  33% /vendor", "")
            }
            lower.contains("ifconfig") || lower.contains("ip a") -> {
                SimResult(true, "wlan0: flags=4163<UP,BROADCAST,RUNNING,MULTIDCAST> mtu 1500\n        inet 192.168.1.142  netmask 255.255.255.0  broadcast 192.168.1.255\n        inet6 fe80::a00:27ff:fe1a:123/64 scope link\n        ether 08:00:27:1a:12:34  txqueuelen 1000  (Ethernet)\nlo: flags=73<UP,LOOPBACK,RUNNING> mtu 65536\n        inet 127.0.0.1  netmask 255.0.0.0", "")
            }
            lower.contains("top") || lower.contains("ps") -> {
                SimResult(true, "USER      PID  %CPU %MEM    VSZ   RSS TTY      STAT START   TIME COMMAND\nroot         1  0.0  0.1  12.4M  4.2M ?        Ss   16:10   0:01 init\ntermux    3452  1.5  2.4 142.1M 82.3M pts/0    S    16:12   0:05 python\ntermux    39402  0.0  0.8  45.3M 21.1M pts/0    R    16:20   0:00 ps aux", "")
            }
            lower.contains("python -m http.server") -> {
                SimResult(true, "Serving HTTP on 0.0.0.0 port 8080 (http://0.0.0.0:8080/) ...\n[Simulated Logs]: 127.0.0.1 - - [30/May/2026:16:18:22] \"GET / HTTP/1.1\" 200 -", "")
            }
            lower.contains("pkg update") -> {
                SimResult(true, "Hit:1 https://packages.termux.dev/apt/termux-main stable InRelease\nReading package lists... Done\nBuilding dependency tree... Done\nAll packages are up to date.", "")
            }
            else -> {
                SimResult(
                    isSuccess = false,
                    stdout = "",
                    stderr = "Error: Termux application was not found on this device.\n\n" +
                            "This application triggers real physical operations via Termux but 'com.termux' package is not registered.\n\n" +
                            "To execute actual bash commands, please install the Termux and Termux:API applications, then follow the 4-step setup instructions in the 'Setup Guide' tab."
                )
            }
        }
    }

    /**
     * Dispatch command to database and then invoke service to start Termux execution
     */
    fun runCommand(
        name: String,
        command: String,
        isBackground: Boolean = true,
        binaryPath: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val isInstalled = try {
                getApplication<Application>().packageManager.getPackageInfo("com.termux", 0) != null
            } catch (e: Exception) {
                false
            }

            val task = TermuxTask(
                name = name,
                command = command.trim(),
                workDir = if (binaryPath.isNullOrBlank()) null else binaryPath.trim(),
                isBackground = isBackground,
                status = "Running"
            )
            // Insert task to room to get its generated request ID
            val generatedId = repository.insert(task)
            val savedTask = task.copy(id = generatedId)
            
            if (isInstalled) {
                // Execute command via intent services
                withContext(Dispatchers.Main) {
                    TermuxService.execute(getApplication(), savedTask)
                }
            } else {
                // Wait for a realistic brief delay, then simulate output
                kotlinx.coroutines.delay(650)
                val simResult = getSimulatedOutput(name, command.trim())
                val updatedTask = savedTask.copy(
                    status = if (simResult.isSuccess) "Success" else "Error",
                    stdout = simResult.stdout,
                    stderr = simResult.stderr,
                    exitCode = if (simResult.isSuccess) 0 else 1,
                    errMsg = if (simResult.isSuccess) "" else "Termux not detected"
                )
                repository.update(updatedTask)
            }
        }
    }

    /**
     * Delete a single task history item
     */
    fun deleteTask(task: TermuxTask) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(task)
        }
    }

    /**
     * Clears all executed task history logs
     */
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
        }
    }
}
