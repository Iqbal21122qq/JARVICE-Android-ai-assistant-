package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.HybridIntelligenceEngine
import com.example.core.model.AiMode
import com.example.core.model.ChatMessage
import com.example.core.model.MessageSender
import com.example.core.model.RootStatus
import com.example.core.model.ScriptTask
import com.example.core.model.SystemTelemetry
import com.example.core.root.RootShellManager
import com.example.core.system.SystemTelemetryCollector
import com.example.core.voice.VoiceAssistantManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JarvisUiState(
    val telemetry: SystemTelemetry = SystemTelemetry(),
    val messages: List<ChatMessage> = emptyList(),
    val terminalLogs: List<String> = emptyList(),
    val scriptTasks: List<ScriptTask> = emptyList(),
    val isProcessing: Boolean = false,
    val currentAiMode: AiMode = AiMode.ONLINE_GEMINI,
    val rootStatus: RootStatus = RootStatus.UNCHECKED,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val audioAmplitude: Float = 0f,
    val continuousListening: Boolean = false,
    val isMuted: Boolean = false,
    val selfHealingLogs: List<String> = emptyList()
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    val rootShellManager = RootShellManager(application)
    val telemetryCollector = SystemTelemetryCollector(application)
    val intelligenceEngine = HybridIntelligenceEngine(application)

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    val voiceManager = VoiceAssistantManager(application) { voiceText ->
        processDirective(voiceText)
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    init {
        // Collect voice state flows
        viewModelScope.launch {
            voiceManager.isListening.collect { listening ->
                _uiState.value = _uiState.value.copy(isListening = listening)
            }
        }
        viewModelScope.launch {
            voiceManager.isSpeaking.collect { speaking ->
                _uiState.value = _uiState.value.copy(isSpeaking = speaking)
            }
        }
        viewModelScope.launch {
            voiceManager.audioAmplitude.collect { amp ->
                _uiState.value = _uiState.value.copy(audioAmplitude = amp)
            }
        }
        viewModelScope.launch {
            voiceManager.continuousListening.collect { continuous ->
                _uiState.value = _uiState.value.copy(continuousListening = continuous)
            }
        }
        viewModelScope.launch {
            voiceManager.isMuted.collect { muted ->
                _uiState.value = _uiState.value.copy(isMuted = muted)
            }
        }

        // Initial setup & diagnostics
        initializeJarvis()

        // Background Telemetry Poller (every 4 seconds)
        viewModelScope.launch {
            while (true) {
                delay(4000)
                refreshTelemetry()
            }
        }
    }

    private fun initializeJarvis() {
        viewModelScope.launch {
            logTerminal("========================================")
            logTerminal("[CORE] J.A.R.V.I.S. Core Boot Sequence Initiated")
            logTerminal("[KERNEL] Initializing Linux Subsystem & Bridge")

            val status = rootShellManager.checkRootStatus()
            _uiState.value = _uiState.value.copy(rootStatus = status)

            logTerminal("[SECURITY] Root Status: ${status.label} - ${status.description}")
            refreshTelemetry()

            // Initial Welcome Message
            val welcomeText = "Good day, sir. J.A.R.V.I.S. is online and integrated with your rooted Android kernel. All subsystems, root shell execution, and hybrid reasoning cores are standing by."
            addMessage(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = welcomeText,
                    aiMode = AiMode.ONLINE_GEMINI
                )
            )
            voiceManager.speak(welcomeText)
        }
    }

    fun refreshTelemetry() {
        viewModelScope.launch {
            val status = _uiState.value.rootStatus
            val telemetry = telemetryCollector.collectTelemetry(status)
            _uiState.value = _uiState.value.copy(telemetry = telemetry)
        }
    }

    fun verifyRoot() {
        viewModelScope.launch {
            logTerminal("[ROOT] Forcing root privilege verification...")
            val newStatus = rootShellManager.checkRootStatus(forceRefresh = true)
            _uiState.value = _uiState.value.copy(rootStatus = newStatus)
            logTerminal("[ROOT] Verified: ${newStatus.label}")

            val reply = "Root verification complete. Device is operating under ${newStatus.label}."
            addMessage(ChatMessage(sender = MessageSender.JARVIS, text = reply))
            voiceManager.speak(reply)
        }
    }

    fun processDirective(directive: String) {
        if (directive.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)

            // Add user message
            addMessage(ChatMessage(sender = MessageSender.USER, text = directive))
            logTerminal("[DIRECTIVE] User: \"$directive\"")

            // Context summary for LLM
            val tel = _uiState.value.telemetry
            val contextSummary = "Root: ${tel.rootStatus.label}, RAM: ${tel.memoryUsedMb}MB/${tel.memoryTotalMb}MB, CPU: ${tel.cpuUsagePercent}%, Battery: ${tel.batteryPercent}% (${tel.batteryTempCelsius}°C)"

            // Query Hybrid Engine (Gemini -> Local LLM -> Offline Heuristic)
            val aiResponse = intelligenceEngine.queryJarvis(directive, contextSummary)
            _uiState.value = _uiState.value.copy(currentAiMode = aiResponse.aiMode)

            logTerminal("[AI-MODE] ${aiResponse.aiMode.label} - Reasoning: ${aiResponse.reasoning}")

            var commandResultString: String? = null
            var executedCmd: String? = null
            var selfHealed = false

            // Execute Shell Command if required
            if (!aiResponse.shellCommand.isNullOrBlank()) {
                executedCmd = aiResponse.shellCommand
                logTerminal("[EXEC-ROOT] $executedCmd")

                val result = rootShellManager.executeCommand(executedCmd, asRoot = true)
                logTerminal("[RESULT] Exit Code: ${result.exitCode} (${result.durationMs}ms)")
                if (result.stdout.isNotBlank()) logTerminal("[STDOUT]\n${result.stdout}")
                if (result.stderr.isNotBlank()) logTerminal("[STDERR] ${result.stderr}")

                commandResultString = if (result.isSuccess) result.stdout else "Error (${result.exitCode}): ${result.stderr}"

                // Self-Healing Trigger: If command failed, attempt auto-correction
                if (!result.isSuccess) {
                    val healingFix = intelligenceEngine.generateSelfHealingFix(executedCmd, result.stderr)
                    if (healingFix != null) {
                        val fixedCmd = healingFix.first
                        val diagnosis = healingFix.second
                        logTerminal("[SELF-HEALING] $diagnosis")
                        logTerminal("[SELF-HEALING RETRY] $fixedCmd")

                        val healedResult = rootShellManager.executeCommand(fixedCmd, asRoot = true)
                        logTerminal("[HEALED RESULT] Exit Code: ${healedResult.exitCode}")
                        if (healedResult.stdout.isNotBlank()) logTerminal("[HEALED STDOUT]\n${healedResult.stdout}")

                        selfHealed = true
                        commandResultString = "Self-Healed Fix:\n${healedResult.stdout}"
                        val healingNotice = "[Self-Healing Protocol] $diagnosis - Auto-executed patch: $fixedCmd"
                        _uiState.value = _uiState.value.copy(
                            selfHealingLogs = _uiState.value.selfHealingLogs + healingNotice
                        )
                    }
                }
            }

            // Execute Generated Python/Shell Script if required
            if (!aiResponse.pythonScript.isNullOrBlank()) {
                val scriptName = "script_${System.currentTimeMillis()}.py"
                logTerminal("[SCRIPT-GEN] Autonomous script written: $scriptName")
                logTerminal("[SCRIPT CODE]\n${aiResponse.pythonScript}")

                val scriptResult = rootShellManager.saveAndRunScript(
                    fileName = scriptName,
                    scriptContent = aiResponse.pythonScript,
                    isPython = true
                )

                logTerminal("[SCRIPT EXEC] Exit Code: ${scriptResult.exitCode} (${scriptResult.durationMs}ms)")
                if (scriptResult.stdout.isNotBlank()) logTerminal("[SCRIPT OUTPUT]\n${scriptResult.stdout}")
                if (scriptResult.stderr.isNotBlank()) logTerminal("[SCRIPT ERROR] ${scriptResult.stderr}")

                val task = ScriptTask(
                    title = "Autonomous Script: ${directive.take(30)}",
                    language = "python",
                    code = aiResponse.pythonScript,
                    lastRunTimestamp = System.currentTimeMillis(),
                    lastResult = scriptResult,
                    isAutoHealed = selfHealed
                )
                _uiState.value = _uiState.value.copy(
                    scriptTasks = listOf(task) + _uiState.value.scriptTasks
                )
                commandResultString = scriptResult.stdout.ifBlank { scriptResult.stderr }
            }

            // Synthesize voice reply
            voiceManager.speak(aiResponse.spokenResponse)

            // Add response to chat
            addMessage(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = aiResponse.spokenResponse,
                    commandExecuted = executedCmd,
                    commandOutput = commandResultString,
                    scriptCode = aiResponse.pythonScript,
                    isSelfHealed = selfHealed,
                    aiMode = aiResponse.aiMode
                )
            )

            _uiState.value = _uiState.value.copy(isProcessing = false)
            refreshTelemetry()
        }
    }

    fun executeManualCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            logTerminal("[MANUAL-EXEC] $command")
            val result = rootShellManager.executeCommand(command, asRoot = true)
            logTerminal("[EXIT-CODE] ${result.exitCode} (${result.durationMs}ms)")
            if (result.stdout.isNotBlank()) logTerminal("[STDOUT]\n${result.stdout}")
            if (result.stderr.isNotBlank()) logTerminal("[STDERR]\n${result.stderr}")

            val summary = if (result.isSuccess) {
                "Command executed successfully: exit 0"
            } else {
                "Command failed with exit code ${result.exitCode}"
            }

            addMessage(
                ChatMessage(
                    sender = MessageSender.JARVIS,
                    text = summary,
                    commandExecuted = command,
                    commandOutput = result.stdout.ifBlank { result.stderr }
                )
            )
            refreshTelemetry()
        }
    }

    fun triggerMemoryBoost() {
        processDirective("Purge kernel memory cache and optimize RAM")
    }

    fun triggerCpuBoost() {
        processDirective("Query CPU frequency scaling and thermal profile")
    }

    fun toggleContinuousListening() {
        voiceManager.toggleContinuousListening()
    }

    fun toggleMute() {
        voiceManager.toggleMute()
    }

    fun toggleListening() {
        if (_uiState.value.isListening) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening()
        }
    }

    fun clearTerminal() {
        _uiState.value = _uiState.value.copy(terminalLogs = emptyList())
        logTerminal("[TERMINAL] Logs cleared.")
    }

    private fun addMessage(msg: ChatMessage) {
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + msg
        )
    }

    private fun logTerminal(log: String) {
        val timestamp = timeFormat.format(Date())
        val entry = "[$timestamp] $log"
        _uiState.value = _uiState.value.copy(
            terminalLogs = (_uiState.value.terminalLogs + entry).takeLast(150)
        )
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
