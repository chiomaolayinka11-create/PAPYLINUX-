package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.AgentCommand
import com.example.data.model.TermuxTask
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatMessage
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Cognitive Agent", "Presets & Controls", "Console Logs", "Setup Guide")
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var isTermuxInstalled by remember { mutableStateOf(false) }
    var isTermuxApiInstalled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isTermuxInstalled = try {
            context.packageManager.getPackageInfo("com.termux", 0) != null
        } catch (e: Exception) {
            false
        }
        isTermuxApiInstalled = try {
            context.packageManager.getPackageInfo("com.termux.api", 0) != null
        } catch (e: Exception) {
            false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC)) // Elite light slate/blue base background
    ) {
        // App Header
        Surface(
            tonalElevation = 2.dp,
            color = Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Round container with indigo background and code/terminal icon
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF4F46E5), RoundedCornerShape(20.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Termux Bridge",
                                color = Color(0xFF0F172A),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val statusText = when {
                                    isTermuxInstalled && isTermuxApiInstalled -> "Fully Connected"
                                    isTermuxInstalled -> "Termux Only (API Missing)"
                                    else -> "Requires Setup"
                                }
                                val statusColor = when {
                                    isTermuxInstalled && isTermuxApiInstalled -> Color(0xFF10B981) // Green
                                    isTermuxInstalled -> Color(0xFFF59E0B) // Amber
                                    else -> Color(0xFFEF4444) // Red
                                }
                                val statusTextColor = when {
                                    isTermuxInstalled && isTermuxApiInstalled -> Color(0xFF059669)
                                    isTermuxInstalled -> Color(0xFFD97706)
                                    else -> Color(0xFFDC2626)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(statusColor, RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = statusText,
                                    color = statusTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // User Profile Icon Button placeholder
                    IconButton(
                        onClick = { /* Settings / User meta */ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User Account",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Navigation Tabs with Custom Light Styling
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFFFFFFFF),
            contentColor = Color(0xFF4F46E5),
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    selectedContentColor = Color(0xFF4F46E5),
                    unselectedContentColor = Color(0xFF64748B),
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        // View Router
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> AgentChatTab(viewModel = viewModel)
                1 -> TerminalPresetTab(viewModel = viewModel)
                2 -> ConsoleLogsTab(viewModel = viewModel, tasks = tasks)
                3 -> SetupGuideTab()
            }
        }
    }
}

// ==================== TAB 0: AGENT CHAT TAB ====================
@Composable
fun AgentChatTab(viewModel: MainViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isThinking by viewModel.isAgentThinking.collectAsStateWithLifecycle()
    val agentError by viewModel.agentError.collectAsStateWithLifecycle()
    var promptInput by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()

    // Auto scroll list state when message list changes size
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High intensity system agent greeting card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4F46E5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "SYSTEM AGENT",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Aura v2.4",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "PID: 29401",
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Standing by. Active local model loaded correctly. I have root read access to Termux storage systems and local command scripts.",
                                color = Color(0xFFE0E7FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            items(messages) { message ->
                ChatMessageBubble(message = message, onRunCommand = { cmdName, cmdString, isBg ->
                    viewModel.runCommand(cmdName, cmdString, isBg)
                })
            }

            if (isThinking) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Aura is structuring shell flow...",
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LinearProgressIndicator(
                                color = Color(0xFF4F46E5),
                                trackColor = Color(0xFFE2E8F0),
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(4.dp)
                            )
                        }
                    }
                }
            }

            if (agentError != null) {
                item {
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = agentError ?: "",
                                color = Color(0xFF991B1B),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Chat Input floating box with soft shadows
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color(0xFFEFF1F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    placeholder = {
                        Text(
                            "Command Aura...",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        cursorColor = Color(0xFF4F46E5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        if (promptInput.trim().isNotEmpty()) {
                            viewModel.sendUserPrompt(promptInput)
                            promptInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF4F46E5), RoundedCornerShape(22.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage, onRunCommand: (String, String, Boolean) -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val isUser = message.sender == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Label header for sender
        Text(
            text = if (isUser) "USER PROMPT" else "COGNITIVE AGENT",
            color = if (isUser) Color(0xFF64748B) else Color(0xFF4F46E5),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp, start = 6.dp, end = 6.dp)
        )

        // Text content bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) Color(0xFFEBF0FF) else Color(0xFFFFFFFF),
            border = BorderStroke(1.dp, if (isUser) Color(0xFFC7D2FE) else Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    color = Color(0xFF0F172A),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Render execution action cards parsed from the query
                if (message.commands.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "SUGGESTED EXECUTIONS",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    message.commands.forEachIndexed { i, cmdObj ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${i + 1}. ${cmdObj.description}",
                                    color = Color(0xFF475569),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                // Custom Monospace Code terminal look
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = cmdObj.cmd,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF34D399),
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onRunCommand(cmdObj.description, cmdObj.cmd, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Run Background",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Run Background", fontSize = 11.sp, color = Color.White)
                                    }

                                    OutlinedButton(
                                        onClick = { onRunCommand(cmdObj.description, cmdObj.cmd, false) },
                                        border = BorderStroke(1.dp, Color(0xFF4F46E5)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Run Script Window", fontSize = 11.sp, color = Color(0xFF4F46E5))
                                    }

                                    IconButton(
                                        onClick = { clipboardManager.setText(AnnotatedString(cmdObj.cmd)) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy script",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== TAB 1: TERMINAL RUNNER / PRESET TAB ====================
@Composable
fun TerminalPresetTab(viewModel: MainViewModel) {
    var customCommand by remember { mutableStateOf("") }
    var shellBinary by remember { mutableStateOf("/data/data/com.termux/files/usr/bin/bash") }
    var runInBackground by remember { mutableStateOf(true) }

    // Text to speech state
    var ttsText by remember { mutableStateOf("Termux AI system initialized.") }

    // Push notification state
    var notifTitle by remember { mutableStateOf("Device Bridge Alert") }
    var notifBody by remember { mutableStateOf("Local background service is monitoring active sessions.") }

    // Vibrate duration state
    var vibrateMs by remember { mutableStateOf("300") }

    // Volume level states
    var volumeLevel by remember { mutableStateOf("8") }
    var selectedVolumeStream by remember { mutableStateOf("music") }

    val presets = listOf(
        PresetCommand("List Home Files", "ls -la", "Lists comprehensive setup files inside termux directory"),
        PresetCommand("Disk Allocation", "df -h", "Shows current storage usage across systems"),
        PresetCommand("Network Config", "ifconfig || ip a", "Displays network interfaces and IP allocation metrics"),
        PresetCommand("Task Processes", "top -n 1 || ps aux | head -n 15", "Retrieves snapshot of active CPU tasks and processes"),
        PresetCommand("Python Server", "python -m http.server 8080", "Starts a local HTTP server at port 8080"),
        PresetCommand("System Packages", "pkg update && pkg upgrade -y", "Triggers native Termux package upgrades")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. DEVICE HARDWARE CONTROL DASHBOARD HEADER ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF4F46E5), RoundedCornerShape(18.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Api Info",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Make sure you completed Step 3 in the Setup Guide. Direct controls dispatch real, native Android hardware Intents via physical Termux commands.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- 2. PHYSICAL PHONE HARDWARE CONTROLLER PANEL ---
        item {
            Text(
                text = "Physical Phone Controls",
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // (A) TEXT TO SPEECH SPEAKER CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Speech Synthesizer (TTS)", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ttsText,
                        onValueChange = { ttsText = it },
                        placeholder = { Text("What should your phone speak?", fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val sanitized = ttsText.replace("\"", "\\\"")
                            viewModel.runCommand("Phrase Speech Integration", "termux-tts-speak \"$sanitized\"")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Speak Phrase Trigger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // (B) PUSH NOTIFICATION POSTER CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Device Notification Alert", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notifTitle,
                        onValueChange = { notifTitle = it },
                        label = { Text("Notification Title", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notifBody,
                        onValueChange = { notifBody = it },
                        label = { Text("Notification Body Text", fontSize = 11.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val tSanitized = notifTitle.replace("\"", "\\\"")
                            val bSanitized = notifBody.replace("\"", "\\\"")
                            viewModel.runCommand("Device Alert Notification", "termux-notification -t \"$tSanitized\" -c \"$bSanitized\"")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Post Push Notification", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // (C) HARDWARE INTEGRATED SWITCHES CARD (Flashlight, Brightness, Vibe)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Build, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("State Switches & Modulations", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    // Flashlight
                    Text("Flashlight (Torch)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runCommand("Flashlight On", "termux-torch on") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Switch ON", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.runCommand("Flashlight Off", "termux-torch off") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Switch OFF", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Brightness Presets
                    Text("Display Screen Brightness", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "Dim (10)" to "10",
                            "Low (64)" to "64",
                            "Medium" to "128",
                            "High (200)" to "200",
                            "Max (255)" to "255"
                        ).forEach { (label, value) ->
                            Button(
                                onClick = { viewModel.runCommand("Brightness Adjustment", "termux-brightness $value") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                            ) {
                                Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Haptic Vibrator
                    Text("Haptic Device Vibration", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = vibrateMs,
                            onValueChange = { vibrateMs = it },
                            label = { Text("Duration (ms)", fontSize = 10.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val ms = vibrateMs.toIntOrNull() ?: 300
                                viewModel.runCommand("Trigger Device Vibration", "termux-vibrate -d $ms")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                        ) {
                            Text("Trigger Vibrate", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // (D) AUDIO VOLUME CONTROLLER CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audio Volumetric Controllers", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Target Volume Stream", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val streams = listOf("music", "ring", "alarm", "system", "notification")
                        streams.forEach { stream ->
                            val isSelected = selectedVolumeStream == stream
                            Button(
                                onClick = { selectedVolumeStream = stream },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                    contentColor = if (isSelected) Color.White else Color(0xFF475569)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp)
                            ) {
                                Text(stream, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = volumeLevel,
                            onValueChange = { volumeLevel = it },
                            label = { Text("Level (0-15)", fontSize = 10.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4F46E5),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val vol = volumeLevel.toIntOrNull() ?: 8
                                viewModel.runCommand("Modify Volume stream", "termux-volume $selectedVolumeStream $vol")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(56.dp)
                        ) {
                            Text("Set Volume", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // (E) QUICK HARDWARE SENSORS & METRICS QUERY PROBES
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Read Sensors & Device Metrics", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 14.sp)
                    }
                    Text(
                        text = "Launches asynchronous background probes to extract physical device properties and storage. Log outputs stream to local DB state.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.runCommand("Query Battery State", "termux-battery-status") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2F6), contentColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Battery Status", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.runCommand("Query Location Coordinates", "termux-location") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2F6), contentColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GPS Coordinates", fontSize = 11.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.runCommand("Fetch Contacts", "termux-contact-list | head -n 12") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2F6), contentColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Contact Sample", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { viewModel.runCommand("Read Raw Clipboard", "termux-clipboard-get") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEF2F6), contentColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clipboard Reader", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 3. CUSTOM SCRIPT RUNNER PANEL ---
        item {
            Text(
                text = "Manual Terminal Runner",
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Script Shell command",
                        color = Color(0xFF4F46E5),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customCommand,
                        onValueChange = { customCommand = it },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A)),
                        placeholder = {
                            Text("e.g. echo 'Hello Termux' && pwd", fontFamily = FontFamily.Monospace, color = Color(0xFF94A3B8), fontSize = 12.sp)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Executable binary path",
                        color = Color(0xFF475569),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = shellBinary,
                        onValueChange = { shellBinary = it },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = Color(0xFF0F172A), fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF4F46E5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Run in background",
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Background logs are saved reactively",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = runInBackground,
                            onCheckedChange = { runInBackground = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFFFFFFF),
                                checkedTrackColor = Color(0xFF4F46E5),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFFE2E8F0)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (customCommand.trim().isNotEmpty()) {
                                viewModel.runCommand(
                                    name = "Manual Command",
                                    command = customCommand,
                                    isBackground = runInBackground,
                                    binaryPath = shellBinary
                                )
                                customCommand = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dispatch Command Sequence", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        // --- 4. PRESET ACTIONS PANEL ---
        item {
            Text(
                text = "Preset System Automation Scripts",
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        items(presets) { preset ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.runCommand(
                            name = preset.name,
                            command = preset.command,
                            isBackground = true,
                            binaryPath = null
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preset.name,
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = preset.desc,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = preset.command,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = {
                            viewModel.runCommand(
                                name = preset.name,
                                command = preset.command,
                                  isBackground = true,
                                binaryPath = null
                            )
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFE0E7FF), RoundedCornerShape(22.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run Preset",
                            tint = Color(0xFF4F46E5)
                        )
                    }
                }
            }
        }
    }
}

data class PresetCommand(val name: String, val command: String, val desc: String)

// ==================== TAB 2: TERMINAL CONSOLE LOGS TAB ====================
@Composable
fun ConsoleLogsTab(viewModel: MainViewModel, tasks: List<TermuxTask>) {
    var expandedTaskId by remember { mutableStateOf<Int?>(null) }
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Execution History logs",
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            if (tasks.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear all", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "No tasks",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No executions yet",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Trigger command flows from AI Chat or Terminal presets.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    val isExpanded = expandedTaskId == task.id
                    val formatter = SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault())
                    val timeString = formatter.format(Date(task.timestamp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedTaskId = if (isExpanded) null else task.id
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = task.name,
                                        color = Color(0xFF0F172A),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$timeString  •  ${if (task.isBackground) "Background Mode" else "Terminal Window"}",
                                        color = Color(0xFF64748B),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Status Indicator Pill
                                val (textColor, bgColor, borderStrokeColor, text) = when (task.status) {
                                    "Running" -> Quadruple(Color(0xFF0369A1), Color(0xFFE0F2FE), Color(0xFF0284C7), "RUNNING")
                                    "Success" -> Quadruple(Color(0xFF059669), Color(0xFFD1FAE5), Color(0xFF10B981), "SUCCESS")
                                    "Error" -> Quadruple(Color(0xFFDC2626), Color(0xFFFEE2E2), Color(0xFFF87171), "ERROR")
                                    else -> Quadruple(Color(0xFF475569), Color(0xFFF1F5F9), Color(0xFF94A3B8), "PENDING")
                                }

                                Surface(
                                    color = bgColor,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, borderStrokeColor)
                                ) {
                                    Text(
                                        text = text,
                                        color = textColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom code preview box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "$ " + task.command,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    maxLines = if (isExpanded) 100 else 1
                                )
                            }

                            // Expanded output logs
                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))

                                // Exit meta info block
                                if (task.exitCode != null || task.errCode != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        if (task.exitCode != null) {
                                            Text(
                                                text = "Exit Code: ${task.exitCode}",
                                                fontFamily = FontFamily.Monospace,
                                                color = if (task.exitCode == 0) Color(0xFF059669) else Color(0xFFDC2626),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (task.errCode != null) {
                                            Text(
                                                text = "Err Code: ${task.errCode}",
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFDC2626),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Exception logs if present
                                if (task.errMsg.isNotEmpty()) {
                                    Text(
                                        text = "System Exception Logs:",
                                        color = Color(0xFF991B1B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFEE2E2))
                                            .border(BorderStroke(1.dp, Color(0xFFFCA5A5)), RoundedCornerShape(4.dp))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = task.errMsg,
                                            color = Color(0xFF991B1B),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // stdout console
                                Text(
                                    text = "Standard Output (stdout):",
                                    color = Color(0xFF0F172A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = task.stdout.ifBlank { "[Empty output logs]" },
                                        fontFamily = FontFamily.Monospace,
                                        color = if (task.stdout.isBlank()) Color(0xFF64748B) else Color(0xFF34D399),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // stderr console
                                if (task.stderr.isNotBlank()) {
                                    Text(
                                        text = "Standard Error (stderr):",
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                            .border(BorderStroke(1.dp, Color(0xFFDC2626)))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = task.stderr,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFFCA5A5),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Interactive footer actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(onClick = {
                                            val fullLog = "Command: ${task.command}\nStatus: ${task.status}\nExitCode: ${task.exitCode}\nStdout: ${task.stdout}\nStderr: ${task.stderr}"
                                            clipboardManager.setText(AnnotatedString(fullLog))
                                        }) {
                                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy full log", tint = Color(0xFF64748B))
                                        }
                                        IconButton(onClick = { viewModel.runCommand(task.name, task.command, task.isBackground, task.workDir) }) {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Re-execute", tint = Color(0xFF4F46E5))
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteTask(task) }
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete item", tint = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple Quadruple helper data container for our dynamic badge mappings
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ==================== TAB 3: SETUP GUIDE TAB ====================
@Composable
fun SetupGuideTab() {
    val steps = listOf(
        GuideItem(
            title = "1. Enable 'Allow External Apps' in Termux",
            content = "For security reasons, Termux requires users to explicitly authorize external apps. Open the Termux application and execute coordinates to enable it:\n\n" +
                    "echo \"allow-external-apps = true\" >> ~/.termux/termux.properties\n" +
                    "termux-reload-settings"
        ),
        GuideItem(
            title = "2. Turn ON Background Display Permission",
            content = "Android versions 10 and above prevent background processes from running unless Termux has system display permission. Go to:\n\n" +
                    "Android Settings ➔ Apps ➔ Termux ➔ Draw over other apps (or Display over other apps) ➔ Set to ALLOW."
        ),
        GuideItem(
            title = "3. Install Termux:API Companion Package",
            content = "To unlock full physical phone hardware control (flashlight, vibrator, text-to-speech, volume, brightness, notifications, location, system metrics):\n\n" +
                    "1. Install the 'Termux:API' application (F-Droid / Play Store).\n" +
                    "2. Open your Termux shell and run the installer:\n\n" +
                    "pkg install termux-api"
        ),
        GuideItem(
            title = "4. Confirm Connection",
            content = "That is it! Now check 'Presets & Controls' to toggle physical devices, run terminal sequences, or converse with the local Cognitive Agent. Commands are logged dynamically!"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Termux Integration Setup Walkthrough",
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Follow these two quick setup items on your phone to authorize integrations between our app and physical Termux.",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        items(steps) { step ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = step.title,
                        color = Color(0xFF4F46E5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = step.content,
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

data class GuideItem(val title: String, val content: String)
