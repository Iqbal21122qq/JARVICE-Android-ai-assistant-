package com.example.core.model

import java.util.UUID

enum class RootStatus(val label: String, val description: String) {
    ROOTED_MAGISK("ROOT ACTIVE (MAGISK)", "Magisk su daemon detected"),
    ROOTED_KERNELSU("ROOT ACTIVE (KERNELSU)", "KernelSU kernel-level root active"),
    ROOTED_GENERIC("ROOT ACTIVE (SU)", "Standard root binaries detected"),
    NON_ROOT("UNROOTED / SANDBOX", "Restricted to user permissions"),
    UNCHECKED("DETECTING...", "Scanning system binaries")
}

enum class AiMode(val label: String, val subtitle: String) {
    ONLINE_GEMINI("ONLINE (GEMINI)", "Gemini 3.5 Flash Reasoning Engine"),
    OFFLINE_LOCAL_LLM("OFFLINE (LOCAL LLM)", "Termux / Ollama Bridge (127.0.0.1:11434)"),
    OFFLINE_HEURISTIC("OFFLINE (AUTONOMOUS CORE)", "Internal System Intent Parser")
}

data class CommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val isRoot: Boolean
) {
    val isSuccess: Boolean get() = exitCode == 0
}

data class SystemTelemetry(
    val cpuUsagePercent: Int = 0,
    val cpuFreqMhz: Int = 0,
    val memoryUsedMb: Long = 0,
    val memoryTotalMb: Long = 0,
    val batteryPercent: Int = 0,
    val batteryTempCelsius: Float = 0f,
    val isCharging: Boolean = false,
    val storageFreeGb: Float = 0f,
    val rootStatus: RootStatus = RootStatus.UNCHECKED,
    val activeProcessesCount: Int = 0
)

enum class MessageSender {
    USER, JARVIS, SYSTEM
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val commandExecuted: String? = null,
    val commandOutput: String? = null,
    val scriptCode: String? = null,
    val isSelfHealed: Boolean = false,
    val aiMode: AiMode = AiMode.ONLINE_GEMINI
)

data class ScriptTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val language: String, // "bash" or "python"
    val code: String,
    val lastRunTimestamp: Long = 0L,
    val lastResult: CommandResult? = null,
    val isAutoHealed: Boolean = false
)
