package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TechDark
import com.example.ui.theme.TechMuted
import com.example.ui.theme.TechWhite

@Composable
fun TerminalPanel(
    logs: List<String>,
    onExecuteCommand: (String) -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new logs
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JarvisSurface)
            .border(1.dp, JarvisCardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("terminal_panel")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Terminal Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Terminal",
                        tint = ArcCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ROOT SHELL CONSOLE (su -c)",
                        color = TechWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier.size(24.dp).testTag("clear_terminal_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Logs",
                        tint = TechMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Shell Commands Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickCommandChip(label = "id && whoami", cmd = "id && whoami", onExecute = onExecuteCommand)
                QuickCommandChip(label = "uname -a", cmd = "uname -a", onExecute = onExecuteCommand)
                QuickCommandChip(label = "df -h /data", cmd = "df -h /data", onExecute = onExecuteCommand)
                QuickCommandChip(label = "dumpsys battery", cmd = "dumpsys battery | head -n 8", onExecute = onExecuteCommand)
                QuickCommandChip(label = "ps -ef", cmd = "ps -ef | head -n 10", onExecute = onExecuteCommand)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Terminal Log Viewer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisBackground)
                    .border(1.dp, Color(0xFF132238), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Terminal ready. Enter command or voice directive.",
                        color = TechMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(logs) { log ->
                            val color = when {
                                log.contains("[STDERR]") || log.contains("Error") -> AlertRed
                                log.contains("[SELF-HEALING]") || log.contains("[SELF-HEALING RETRY]") -> AlertAmber
                                log.contains("[EXEC-ROOT]") || log.contains("[DIRECTIVE]") -> ArcCyan
                                log.contains("[SCRIPT-GEN]") -> ArcBlue
                                log.contains("[STDOUT]") || log.contains("[HEALED RESULT]") -> SuccessGreen
                                else -> TechWhite.copy(alpha = 0.85f)
                            }
                            Text(
                                text = log,
                                color = color,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Command Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = {
                        Text(
                            text = "e.g., su -c 'ls -la /data'",
                            color = TechMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_command_input"),
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
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (commandInput.isNotBlank()) {
                            onExecuteCommand(commandInput)
                            commandInput = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            onExecuteCommand(commandInput)
                            commandInput = ""
                        }
                    },
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("run_command_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcCyan,
                        contentColor = TechDark
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run Command",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCommandChip(
    label: String,
    cmd: String,
    onExecute: (String) -> Unit
) {
    SuggestionChip(
        onClick = { onExecute(cmd) },
        label = {
            Text(
                text = label,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = ArcCyan
            )
        },
        shape = RoundedCornerShape(6.dp),
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFF0D1B2E)
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = JarvisCardBorder
        )
    )
}
