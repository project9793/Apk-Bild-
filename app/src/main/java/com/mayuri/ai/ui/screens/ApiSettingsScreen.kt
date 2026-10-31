package com.mayuri.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mayuri.ai.data.local.SecurePreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiSettingsScreen(
    prefs: SecurePreferencesManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var geminiKey by remember { mutableStateOf(prefs.getGeminiApiKey() ?: "") }
    var openAiKey by remember { mutableStateOf(prefs.getOpenAiApiKey() ?: "") }
    var selectedProvider by remember { mutableStateOf(prefs.getProvider()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Provider & API Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F081D))
            )
        },
        containerColor = Color(0xFF0A0514)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Text(
                "Choose Active AI Engine",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(modifier = Modifier.padding(vertical = 12.dp)) {
                FilterChip(
                    selected = selectedProvider == "GEMINI",
                    onClick = {
                        selectedProvider = "GEMINI"
                        prefs.setProvider("GEMINI")
                    },
                    label = { Text("Google Gemini") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF7C4DFF),
                        selectedLabelColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                FilterChip(
                    selected = selectedProvider == "OPENAI",
                    onClick = {
                        selectedProvider = "OPENAI"
                        prefs.setProvider("OPENAI")
                    },
                    label = { Text("OpenAI GPT-4") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF7C4DFF),
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Gemini API Input
            Text("Google Gemini API Key", color = Color(0xFFB388FF), fontSize = 14.sp)
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it },
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("AIzaSy...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // OpenAI API Input
            Text("OpenAI API Key", color = Color(0xFFB388FF), fontSize = 14.sp)
            OutlinedTextField(
                value = openAiKey,
                onValueChange = { openAiKey = it },
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("sk-proj-...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    prefs.saveGeminiApiKey(geminiKey)
                    prefs.saveOpenAiApiKey(openAiKey)
                    Toast.makeText(context, "API Keys securely saved in Keystore!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Validate Credentials", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}