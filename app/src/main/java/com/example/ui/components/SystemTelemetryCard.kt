package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.AiMode
import com.example.core.model.RootStatus
import com.example.core.model.SystemTelemetry
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.RootOrange
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TechMuted
import com.example.ui.theme.TechWhite

@Composable
fun SystemTelemetryCard(
    telemetry: SystemTelemetry,
    currentAiMode: AiMode,
    onVerifyRoot: () -> Unit,
    onBoostRam: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JarvisSurface)
            .border(1.dp, JarvisCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("telemetry_hud_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Root Status Badge + AI Mode Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Root Badge
                val isRooted = telemetry.rootStatus == RootStatus.ROOTED_MAGISK ||
                        telemetry.rootStatus == RootStatus.ROOTED_KERNELSU ||
                        telemetry.rootStatus == RootStatus.ROOTED_GENERIC

                val rootBadgeColor = if (isRooted) SuccessGreen else AlertRed

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(rootBadgeColor.copy(alpha = 0.15f))
                        .border(1.dp, rootBadgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(rootBadgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = telemetry.rootStatus.label,
                        color = rootBadgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // AI Mode Pill
                val modeColor = when (currentAiMode) {
                    AiMode.ONLINE_GEMINI -> ArcCyan
                    AiMode.OFFLINE_LOCAL_LLM -> ArcBlue
                    AiMode.OFFLINE_HEURISTIC -> RootOrange
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(modeColor.copy(alpha = 0.15f))
                        .border(1.dp, modeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentAiMode.label,
                        color = modeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Grid: CPU, RAM, Battery, Storage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniItem(
                    icon = Icons.Default.Speed,
                    label = "CPU LOAD",
                    value = "${telemetry.cpuUsagePercent}%",
                    subValue = "${telemetry.cpuFreqMhz} MHz",
                    tint = ArcCyan,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniItem(
                    icon = Icons.Default.Memory,
                    label = "MEMORY",
                    value = "${telemetry.memoryUsedMb} MB",
                    subValue = "of ${telemetry.memoryTotalMb} MB",
                    tint = ArcBlue,
                    modifier = Modifier.weight(1f)
                )

                MetricMiniItem(
                    icon = Icons.Default.BatteryChargingFull,
                    label = "BATTERY",
                    value = "${telemetry.batteryPercent}%",
                    subValue = "${telemetry.batteryTempCelsius}°C",
                    tint = if (telemetry.isCharging) SuccessGreen else TechWhite,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // RAM Progress Bar
            val ramFraction = if (telemetry.memoryTotalMb > 0) {
                (telemetry.memoryUsedMb.toFloat() / telemetry.memoryTotalMb.toFloat()).coerceIn(0f, 1f)
            } else 0.5f

            LinearProgressIndicator(
                progress = { ramFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (ramFraction > 0.85f) AlertRed else ArcCyan,
                trackColor = JarvisCardBorder
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Verify Root & Flush Caches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVerifyRoot,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("verify_root_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ArcCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Verify Root",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "VERIFY SU", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                OutlinedButton(
                    onClick = onBoostRam,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("boost_ram_button"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SuccessGreen
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Flush Memory",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "BOOST RAM", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun MetricMiniItem(
    icon: ImageVector,
    label: String,
    value: String,
    subValue: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF070D18))
            .border(1.dp, JarvisCardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = TechMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = TechWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subValue,
                color = TechMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
