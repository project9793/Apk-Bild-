package com.mayuri.ai.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mayuri.ai.R
import com.mayuri.ai.core.voice.VoiceState
import com.mayuri.ai.ui.components.MayuriGlowingOrb

data class QuickActionItem(val title: String, @DrawableRes val icon: Int, val route: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    voiceState: VoiceState,
    amplitude: Float,
    statusText: String,
    onMicClick: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0612), Color(0xFF130924), Color(0xFF05010B))
    )

    val quickActions = listOf(
        QuickActionItem("Voice Chat", R.drawable.ic_mic, "voice_chat"),
        QuickActionItem("AI Chat", R.drawable.ic_chat, "ai_chat"),
        QuickActionItem("Code Studio", R.drawable.ic_code, "code_studio"),
        QuickActionItem("Social Media", R.drawable.ic_share, "social_assistant"),
        QuickActionItem("API Settings", R.drawable.ic_key, "api_settings"),
        QuickActionItem("History", R.drawable.ic_history, "chat_history")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "MAYURI AI",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigate("api_settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Hello! Main Mayuri hoon.",
                fontSize = 22.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )

            Text(
                text = statusText,
                fontSize = 14.sp,
                color = Color(0xFFB388FF),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // Central Pulsing Orb
            MayuriGlowingOrb(
                voiceState = voiceState,
                amplitude = amplitude,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            // Action Voice Bar / Mic Trigger
            FloatingActionButton(
                onClick = onMicClick,
                shape = CircleShape,
                containerColor = if (voiceState is VoiceState.Listening) Color(0xFFFF1744) else Color(0xFF7C4DFF),
                contentColor = Color.White,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (voiceState is VoiceState.Listening) R.drawable.ic_stop else R.drawable.ic_mic
                    ),
                    contentDescription = "Microphone",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions 2-Column Grid
            Text(
                text = "Quick Actions",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                items(quickActions) { item ->
                    GlassmorphicActionCard(item) { onNavigate(item.route) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun GlassmorphicActionCard(item: QuickActionItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7C4DFF).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(item.icon), contentDescription = item.title, tint = Color(0xFFB388FF), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}