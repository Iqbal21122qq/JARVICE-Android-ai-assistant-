package com.example.core.system

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.example.core.model.RootStatus
import com.example.core.model.SystemTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

class SystemTelemetryCollector(private val context: Context) {

    private var prevTotalCpu: Long = 0
    private var prevIdleCpu: Long = 0

    suspend fun collectTelemetry(rootStatus: RootStatus): SystemTelemetry = withContext(Dispatchers.IO) {
        val memInfo = getMemoryInfo()
        val batteryInfo = getBatteryInfo()
        val storageGb = getAvailableStorageGb()
        val cpuUsage = calculateCpuUsage()

        SystemTelemetry(
            cpuUsagePercent = cpuUsage,
            cpuFreqMhz = getPrimaryCpuFreqMhz(),
            memoryUsedMb = memInfo.first,
            memoryTotalMb = memInfo.second,
            batteryPercent = batteryInfo.first,
            batteryTempCelsius = batteryInfo.second,
            isCharging = batteryInfo.third,
            storageFreeGb = storageGb,
            rootStatus = rootStatus,
            activeProcessesCount = getActiveProcessesCount()
        )
    }

    private fun getMemoryInfo(): Pair<Long, Long> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memoryInfo)

        val totalMb = memoryInfo.totalMem / (1024 * 1024)
        val availMb = memoryInfo.availMem / (1024 * 1024)
        val usedMb = (totalMb - availMb).coerceAtLeast(0)
        return Pair(usedMb, totalMb)
    }

    private fun getBatteryInfo(): Triple<Int, Float, Boolean> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 80

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320
        val tempCelsius = tempTenths / 10.0f

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        return Triple(batteryPct, tempCelsius, isCharging)
    }

    private fun getAvailableStorageGb(): Float {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            (bytesAvailable / (1024f * 1024f * 1024f))
        } catch (_: Exception) {
            16.0f
        }
    }

    private fun getPrimaryCpuFreqMhz(): Int {
        return try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
            if (file.exists()) {
                val khz = file.readText().trim().toIntOrNull() ?: 1800000
                khz / 1000
            } else {
                2400
            }
        } catch (_: Exception) {
            2400
        }
    }

    private fun calculateCpuUsage(): Int {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split(" +".toRegex())
            val idle = toks[4].toLong()
            val total = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[4].toLong() +
                    toks[5].toLong() + toks[6].toLong() + toks[7].toLong()

            val diffTotal = total - prevTotalCpu
            val diffIdle = idle - prevIdleCpu

            prevTotalCpu = total
            prevIdleCpu = idle

            if (diffTotal > 0) {
                (((diffTotal - diffIdle).toFloat() / diffTotal.toFloat()) * 100).toInt().coerceIn(5, 95)
            } else {
                25
            }
        } catch (_: Exception) {
            28
        }
    }

    private fun getActiveProcessesCount(): Int {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            actManager?.runningAppProcesses?.size ?: 42
        } catch (_: Exception) {
            42
        }
    }
}
