package com.mayuri.ai.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeStudioScreen(
    onBack: () -> Unit,
    onGenerateCode: suspend (prompt: String, lang: String) -> Result<String>
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var promptInput by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("Kotlin") }
    var generatedCode by remember { mutableStateOf("// Code output will appear here...") }
    var isLoading by remember { mutableStateOf(false) }

    val languages = listOf("Kotlin", "Java", "Python", "JavaScript", "HTML/CSS")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Code Studio", color = Color.White) },
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
                .padding(16.dp)
        ) {
            // Language selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { lang ->
                    FilterChip(
                        selected = selectedLanguage == lang,
                        onClick = { selectedLanguage = lang },
                        label = { Text(lang) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF7C4DFF),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E1338),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Describe the logic, app, or algorithm to generate...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF7C4DFF),
                    unfocusedBorderColor = Color(0xFF382363),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        isLoading = true
                        coroutineScope.launch {
                            val result = onGenerateCode(promptInput, selectedLanguage)
                            isLoading = false
                            result.onSuccess { generatedCode = it }
                                .onFailure {
                                    Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show()
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Solution")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Code Output Card (IDE-like styling)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF120C24))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Language: $selectedLanguage",
                            color = Color(0xFFB388FF),
                            fontSize = 12.sp
                        )
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(generatedCode))
                            Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2B1A4D))
                    Spacer(modifier = Modifier.height(8.dp))

                    SelectionContainer {
                        Text(
                            text = generatedCode,
                            color = Color(0xFF80D8FF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
    }
}