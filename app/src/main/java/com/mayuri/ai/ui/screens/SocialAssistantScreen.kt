package com.mayuri.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mayuri.ai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialAssistantScreen(
    onBack: () -> Unit,
    onShare: (text: String, packageName: String?) -> Unit
) {
    var topicInput by remember { mutableStateOf("") }
    var captionResult by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Social Media Assistant", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = Color.White)
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
            OutlinedTextField(
                value = topicInput,
                onValueChange = { topicInput = it },
                label = { Text("Enter Video/Post Topic or Message") },
                placeholder = { Text("e.g. Travel Reel in Manali with Friends") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    captionResult = "✨ Lost in the mountains! 🏔️ Exploring the mystical vibes of $topicInput. #TravelDiaries #Wanderlust #MayuriAI"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Generate Viral Captions & Tags")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (captionResult.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1338))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(captionResult, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onShare(captionResult, "com.whatsapp") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Text("WhatsApp")
                            }

                            Button(
                                onClick = { onShare(captionResult, "com.instagram.android") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C))
                            ) {
                                Text("Instagram")
                            }

                            IconButton(onClick = { onShare(captionResult, null) }) {
                                Icon(painterResource(R.drawable.ic_share), contentDescription = "Share", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}