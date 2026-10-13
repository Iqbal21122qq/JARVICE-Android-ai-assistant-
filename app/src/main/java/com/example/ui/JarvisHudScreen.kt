package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HearingDisabled
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.model.RootStatus
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.ArchitectureGuideDialog
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.SystemTelemetryCard
import com.example.ui.components.TerminalPanel
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.HologramTeal
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TechDark
import com.example.ui.theme.TechMuted
import com.example.ui.theme.TechWhite

@Composable
fun JarvisHudScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var textInput by remember { mutableStateOf("") }
    var showArchitectureDialog by remember { mutableStateOf(false) }

    // Audio Permission Launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleListening()
        }
    }

    fun handleMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.toggleListening()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (showArchitectureDialog) {
        ArchitectureGuideDialog(onDismiss = { showArchitectureDialog = false })
    }

    Scaffold(
        modifier = modifier.fillMaxSize().background(JarvisBackground),
        containerColor = JarvisBackground,
        topBar = {
            TopHudBar(
                isContinuous = uiState.continuousListening,
                isMuted = uiState.isMuted,
                onToggleContinuous = { viewModel.toggleContinuousListening() },
                onToggleMute = { viewModel.toggleMute() },
                onShowSpecs = { showArchitectureDialog = true }
            )
        },
        bottomBar = {
            BottomVoiceController(
                isListening = uiState.isListening,
                isSpeaking = uiState.isSpeaking,
                amplitude = uiState.audioAmplitude,
                isProcessing = uiState.isProcessing,
                textInput = textInput,
                onTextInputChange = { textInput = it },
                onSend = {
                    if (textInput.isNotBlank()) {
                        viewModel.processDirective(textInput)
                        textInput = ""
                    }
                },
                onMicClick = { handleMicClick() },
                onQuickDirective = { directive -> viewModel.processDirective(directive) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Hero Arc Reactor Core Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val isRooted = uiState.rootStatus != RootStatus.NON_ROOT && uiState.rootStatus != RootStatus.UNCHECKED
                    ArcReactorVisualizer(
                        isListening = uiState.isListening,
                        isSpeaking = uiState.isSpeaking,
                        amplitude = uiState.audioAmplitude,
                        isRooted = isRooted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Status Indicator Text
                    val statusText = when {
                        uiState.isListening -> "LISTENING // AUDIO RECEPTOR ACTIVE"
                        uiState.isSpeaking -> "SYNTHESIZING VOCAL RESPONSE..."
                        uiState.isProcessing -> "PROCESSING DIRECTIVE (REASONING CORE)..."
                        else -> "SYSTEM NOMINAL // STANDING BY"
                    }
                    val statusColor = when {
                        uiState.isListening -> ArcCyan
                        uiState.isSpeaking -> HologramTeal
                        uiState.isProcessing -> ArcBlue
                        else -> TechMuted
                    }

                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Tabs: DASHBOARD / TERMINAL vs CONVERSATION / SCRIPTS
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = JarvisSurface,
                contentColor = ArcCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ArcCyan,
                        height = 2.dp
                    )
                },
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "HUD & CONSOLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 0) ArcCyan else TechMuted
                        )
                    },
                    modifier = Modifier.testTag("tab_hud_console")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "DIRECTIVES & SCRIPTS (${uiState.messages.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTab == 1) ArcCyan else TechMuted
                        )
                    },
                    modifier = Modifier.testTag("tab_messages_scripts")
                )
            }

            // Content Area
            if (selectedTab == 0) {
                // Tab 0: Telemetry HUD + Terminal Panel
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    SystemTelemetryCard(
                        telemetry = uiState.telemetry,
                        currentAiMode = uiState.currentAiMode,
                        onVerifyRoot = { viewModel.verifyRoot() },
                        onBoostRam = { viewModel.triggerMemoryBoost() }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TerminalPanel(
                        logs = uiState.terminalLogs,
                        onExecuteCommand = { cmd -> viewModel.executeManualCommand(cmd) },
                        onClearLogs = { viewModel.clearTerminal() }
                    )
                }
            } else {
                // Tab 1: Conversational Log & Generated Scripts
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    if (uiState.messages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No directives logged yet. Use the microphone or directive input below.",
                                    color = TechMuted,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        items(uiState.messages) { msg ->
                            ChatMessageItem(message = msg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHudBar(
    isContinuous: Boolean,
    isMuted: Boolean,
    onToggleContinuous: () -> Unit,
    onToggleMute: () -> Unit,
    onShowSpecs: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisSurface)
            .border(1.dp, JarvisCardBorder)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("top_hud_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "J.A.R.V.I.S. // CORE",
                    color = ArcCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "AUTONOMOUS ROOT INTELLIGENCE",
                    color = TechMuted,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Specs / Setup Dialog Button
                IconButton(
                    onClick = onShowSpecs,
                    modifier = Modifier.size(36.dp).testTag("specs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Architecture Specs",
                        tint = ArcCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Continuous Auto-Listen Toggle
                IconButton(
                    onClick = onToggleContinuous,
                    modifier = Modifier.size(36.dp).testTag("toggle_continuous_btn")
                ) {
                    Icon(
                        imageVector = if (isContinuous) Icons.Default.Hearing else Icons.Default.HearingDisabled,
                        contentDescription = "Toggle Continuous Listening",
                        tint = if (isContinuous) SuccessGreen else TechMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // TTS Mute Toggle
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier.size(36.dp).testTag("toggle_mute_btn")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                        contentDescription = "Toggle Audio Mute",
                        tint = if (isMuted) AlertRed else ArcCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomVoiceController(
    isListening: Boolean,
    isSpeaking: Boolean,
    amplitude: Float,
    isProcessing: Boolean,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onQuickDirective: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(JarvisSurface)
            .border(1.dp, JarvisCardBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(12.dp)
            .testTag("bottom_voice_controller")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Quick Directives Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickDirectiveChip("Boost RAM", onQuickDirective)
                QuickDirectiveChip("CPU Telemetry", onQuickDirective)
                QuickDirectiveChip("Run Python Test", onQuickDirective)
                QuickDirectiveChip("Battery Health", onQuickDirective)
                QuickDirectiveChip("Active Processes", onQuickDirective)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Input Bar: Mic Button + TextField + Send Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Button with glow ring when listening
                Box(contentAlignment = Alignment.Center) {
                    if (isListening) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(ArcCyan.copy(alpha = 0.25f + amplitude * 0.35f))
                        )
                    }

                    Button(
                        onClick = onMicClick,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) ArcCyan else Color(0xFF14243B),
                            contentColor = if (isListening) TechDark else ArcCyan
                        ),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("mic_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Voice Directive Input",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = onTextInputChange,
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening to speech..." else "Enter directive or ask J.A.R.V.I.S...",
                            color = TechMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("directive_input_field"),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = TechWhite,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = JarvisCardBorder,
                        focusedContainerColor = JarvisBackground,
                        unfocusedContainerColor = JarvisBackground
                    ),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onSend,
                    enabled = textInput.isNotBlank() && !isProcessing,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcCyan,
                        contentColor = TechDark,
                        disabledContainerColor = Color(0xFF132238),
                        disabledContentColor = TechMuted
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("send_directive_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Directive",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDirectiveChip(
    text: String,
    onClick: (String) -> Unit
) {
    SuggestionChip(
        onClick = { onClick(text) },
        label = {
            Text(
                text = text,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = ArcCyan
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFF0F1E33)
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = JarvisCardBorder
        )
    )
}
