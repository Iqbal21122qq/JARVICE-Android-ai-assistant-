package com.example.core.root

import android.content.Context
import android.os.SystemClock
import com.example.core.model.CommandResult
import com.example.core.model.RootStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class RootShellManager(private val context: Context) {

    private val commonSuPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su",
        "/data/adb/ksu/bin/su"
    )

    private var cachedRootStatus: RootStatus? = null

    suspend fun checkRootStatus(forceRefresh: Boolean = false): RootStatus = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedRootStatus != null && cachedRootStatus != RootStatus.UNCHECKED) {
            return@withContext cachedRootStatus!!
        }

        // 1. Check for SU binaries on file system
        var binaryExists = false
        for (path in commonSuPaths) {
            try {
                if (File(path).exists()) {
                    binaryExists = true
                    break
                }
            } catch (_: Exception) {}
        }

        // 2. Test actual 'su' execution
        val suExecResult = executeCommand("id", asRoot = true, timeoutMs = 3000)
        val hasUid0 = suExecResult.stdout.contains("uid=0(root)") || suExecResult.stdout.contains("root")

        val status = if (hasUid0 || (binaryExists && suExecResult.exitCode == 0)) {
            // Distinguish Magisk vs KernelSU
            val magiskCheck = executeCommand("magisk -v", asRoot = true, timeoutMs = 2000)
            val ksuCheck = executeCommand("ksud -V", asRoot = true, timeoutMs = 2000)

            when {
                magiskCheck.isSuccess && magiskCheck.stdout.isNotBlank() -> RootStatus.ROOTED_MAGISK
                ksuCheck.isSuccess && ksuCheck.stdout.isNotBlank() -> RootStatus.ROOTED_KERNELSU
                else -> RootStatus.ROOTED_GENERIC
            }
        } else {
            RootStatus.NON_ROOT
        }

        cachedRootStatus = status
        status
    }

    suspend fun executeCommand(
        command: String,
        asRoot: Boolean = true,
        timeoutMs: Long = 10000
    ): CommandResult = withContext(Dispatchers.IO) {
        val startTime = SystemClock.elapsedRealtime()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()
        var exitCode = -1

        try {
            val process = if (asRoot) {
                // Execute via su
                ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(false)
                    .start()
            } else {
                // Execute in regular shell
                ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(false)
                    .start()
            }

            // Asynchronously read stdout and stderr
            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdoutThread = Thread {
                try {
                    var line: String?
                    while (outReader.readLine().also { line = it } != null) {
                        stdoutBuilder.appendLine(line)
                    }
                } catch (_: Exception) {}
            }

            val stderrThread = Thread {
                try {
                    var line: String?
                    while (errReader.readLine().also { line = it } != null) {
                        stderrBuilder.appendLine(line)
                    }
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (finished) {
                exitCode = process.exitValue()
                stdoutThread.join(1000)
                stderrThread.join(1000)
            } else {
                process.destroyForcibly()
                stderrBuilder.appendLine("Execution timed out after ${timeoutMs}ms")
                exitCode = -9
            }
        } catch (e: Exception) {
            stderrBuilder.appendLine("Execution failed: ${e.message}")
            exitCode = -1
        }

        val duration = SystemClock.elapsedRealtime() - startTime
        CommandResult(
            command = command,
            exitCode = exitCode,
            stdout = stdoutBuilder.toString().trim(),
            stderr = stderrBuilder.toString().trim(),
            durationMs = duration,
            isRoot = asRoot
        )
    }

    suspend fun saveAndRunScript(
        fileName: String,
        scriptContent: String,
        isPython: Boolean = false
    ): CommandResult = withContext(Dispatchers.IO) {
        val scriptsDir = File(context.cacheDir, "jarvis_scripts")
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs()
        }

        val scriptFile = File(scriptsDir, fileName)
        scriptFile.writeText(scriptContent)
        scriptFile.setExecutable(true, false)

        val command = if (isPython) {
            // Check if python or python3 is available (e.g. Termux or system python)
            "if which python3 > /dev/null 2>&1; then python3 ${scriptFile.absolutePath}; elif which python > /dev/null 2>&1; then python ${scriptFile.absolutePath}; elif [ -f /data/data/com.termux/files/usr/bin/python ]; then /data/data/com.termux/files/usr/bin/python ${scriptFile.absolutePath}; else sh ${scriptFile.absolutePath}; fi"
        } else {
            "sh ${scriptFile.absolutePath}"
        }

        executeCommand(command, asRoot = true, timeoutMs = 15000)
    }

    // --- High-Level System Automation Primitives ---

    suspend fun boostMemory(): CommandResult = withContext(Dispatchers.IO) {
        executeCommand("sync && echo 3 > /proc/sys/vm/drop_caches", asRoot = true)
    }

    suspend fun getCpuInfo(): CommandResult = withContext(Dispatchers.IO) {
        executeCommand("cat /proc/cpuinfo | grep 'model name\\|Hardware\\|processor' | head -n 4", asRoot = false)
    }

    suspend fun getThermalStats(): CommandResult = withContext(Dispatchers.IO) {
        executeCommand("cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || dumpsys battery | grep temperature", asRoot = true)
    }

    suspend fun toggleWifi(enable: Boolean): CommandResult = withContext(Dispatchers.IO) {
        executeCommand("svc wifi ${if (enable) "enable" else "disable"}", asRoot = true)
    }

    suspend fun toggleMobileData(enable: Boolean): CommandResult = withContext(Dispatchers.IO) {
        executeCommand("svc data ${if (enable) "enable" else "disable"}", asRoot = true)
    }

    suspend fun killApp(packageName: String): CommandResult = withContext(Dispatchers.IO) {
        executeCommand("am force-stop $packageName", asRoot = true)
    }

    suspend fun launchApp(packageName: String): CommandResult = withContext(Dispatchers.IO) {
        executeCommand("monkey -p $packageName -c android.intent.category.LAUNCHER 1", asRoot = true)
    }

    suspend fun getTopProcesses(): CommandResult = withContext(Dispatchers.IO) {
        executeCommand("top -b -n 1 -m 8 -s 9 2>/dev/null || ps -ef | head -n 10", asRoot = false)
    }
}
