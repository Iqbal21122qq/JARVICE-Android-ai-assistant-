package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.JarvisBackground
import com.example.ui.theme.JarvisCardBorder
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TechMuted
import com.example.ui.theme.TechWhite

@Composable
fun ArchitectureGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val termuxBootstrap = """
# 1. Update Termux repositories & install root tools
pkg update -y && pkg install -y python root-repo tsu git clang curl

# 2. Install Python dependencies for JARVIS daemon
pip install flask requests psutil

# 3. Setup Ollama (Local LLM Server in Termux / Linux PRoot)
# Run Ollama server locally on port 11434
curl -fsSL https://ollama.com/install.sh | sh
ollama serve &
ollama pull llama3.2:1b
    """.trimIndent()

    val pythonDaemonCode = """
# jarvis_daemon.py - Termux Background Worker
import os, sys, subprocess, psutil
from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "online",
        "cpu_percent": psutil.cpu_percent(),
        "ram_used_mb": psutil.virtual_memory().used // (1024*1024),
        "is_root": os.geteuid() == 0
    })

@app.route('/exec', methods=['POST'])
def execute_root():
    data = request.json or {}
    cmd = data.get('command', '')
    timeout = data.get('timeout', 15)
    try:
        proc = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=timeout)
        return jsonify({
            "exit_code": proc.returncode,
            "stdout": proc.stdout,
            "stderr": proc.stderr
        })
    except subprocess.TimeoutExpired:
        return jsonify({"exit_code": -1, "stderr": "Execution timeout"}), 408

if __name__ == '__main__':
    # Listens on localhost:5000 for Android App requests
    app.run(host='127.0.0.1', port=5000)
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("arch_guide_dialog"),
        containerColor = JarvisSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "Architecture",
                        tint = ArcCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM ARCHITECTURE & SETUP",
                        color = TechWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TechMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "PHASE 1: IPC & INTELLIGENCE CORE BRIDGE",
                    color = ArcCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The app utilizes a dual-path execution pipeline:\n" +
                            "1. Direct Root Subshell (su -c): Commands are piped directly to /system/bin/su via ProcessBuilder with stderr/stdout separation.\n" +
                            "2. Termux / Local LLM Bridge: Connects to Ollama (http://127.0.0.1:11434) and optional Python workers on localhost.\n" +
                            "3. Cloud Gemini API: When online, complex reasoning & agentic planning execute with Gemini 3.5 Flash.",
                    color = TechMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Termux Setup Script
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TERMUX SETUP COMMANDS",
                        color = SuccessGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedButton(
                        onClick = { copyToClipboard(termuxBootstrap, "Termux Setup") },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "COPY", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(JarvisBackground)
                        .border(1.dp, Color(0xFF132238), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = termuxBootstrap,
                        color = TechWhite.copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section: Python Background Daemon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PYTHON WORKER DAEMON (LOCAL)",
                        color = ArcBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedButton(
                        onClick = { copyToClipboard(pythonDaemonCode, "Python Daemon") },
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcBlue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcBlue.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "COPY", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(JarvisBackground)
                        .border(1.dp, Color(0xFF132238), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = pythonDaemonCode,
                        color = TechWhite.copy(alpha = 0.9f),
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "DISMISS", color = ArcCyan, fontFamily = FontFamily.Monospace)
            }
        }
    )
}
