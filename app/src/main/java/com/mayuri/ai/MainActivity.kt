package com.mayuri.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mayuri.ai.ui.screens.*
import com.mayuri.ai.ui.viewmodels.MayuriVoiceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MayuriVoiceViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (!audioGranted) {
            Toast.makeText(this, "Microphone permission zaroori hai voice assistant ke liye.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request runtime permissions on launch
        checkAndRequestPermissions()

        setContent {
            val navController = rememberNavController()
            val voiceState by viewModel.voiceState.collectAsState()
            val amplitude by viewModel.rmsLevel.collectAsState()
            val statusText by viewModel.statusText.collectAsState()
            val pendingCallIntent by viewModel.pendingCallIntent.collectAsState()

            // Handle direct phone call intents triggered by voice commands
            LaunchedEffect(pendingCallIntent) {
                pendingCallIntent?.let { intent ->
                    startActivity(intent)
                    viewModel.clearPendingCall()
                }
            }

            NavHost(navController = navController, startDestination = "splash") {
                composable("splash") {
                    SplashScreen(
                        onFinished = {
                            navController.navigate("home") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        voiceState = voiceState,
                        amplitude = amplitude,
                        statusText = statusText,
                        onMicClick = { viewModel.toggleListening() },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }

                composable("voice_chat") {
                    VoiceChatScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("ai_chat") {
                    AiChatScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("chat_history") {
                    ChatHistoryScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("code_studio") {
                    CodeStudioScreen(
                        onBack = { navController.popBackStack() },
                        onGenerateCode = { prompt, lang -> viewModel.generateCode(prompt, lang) }
                    )
                }

                composable("api_settings") {
                    ApiSettingsScreen(
                        prefs = viewModel.prefs,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("social_assistant") {
                    SocialAssistantScreen(
                        onBack = { navController.popBackStack() },
                        onShare = { text, pkg -> viewModel.actionHandler.shareSocialContent(text, pkg) }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}