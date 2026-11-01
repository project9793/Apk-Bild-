package com.mayuri.ai.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mayuri.ai.R
import com.mayuri.ai.core.voice.VoiceState
import com.mayuri.ai.ui.components.MayuriGlowingOrb
import com.mayuri.ai.ui.viewmodels.MayuriVoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(
    viewModel: MayuriVoiceViewModel,
    onBack: () -> Unit
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val amplitude by viewModel.rmsLevel.collectAsState()
    val statusText by viewModel.statusText.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Chat", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Hello! Main Mayuri hoon.",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                statusText,
                color = Color(0xFFB388FF),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.weight(0.4f))

            MayuriGlowingOrb(voiceState = voiceState, amplitude = amplitude)

            Spacer(Modifier.weight(0.4f))

            FloatingActionButton(
                onClick = { viewModel.toggleListening() },
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

            Spacer(Modifier.height(24.dp))
        }
    }
}
