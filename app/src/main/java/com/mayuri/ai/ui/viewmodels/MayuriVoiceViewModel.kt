package com.mayuri.ai.ui.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mayuri.ai.core.actions.ActionResponse
import com.mayuri.ai.core.actions.DeviceActionHandler
import com.mayuri.ai.core.voice.VoiceManager
import com.mayuri.ai.core.voice.VoiceState
import com.mayuri.ai.data.local.ChatEntity
import com.mayuri.ai.data.local.MayuriDatabase
import com.mayuri.ai.data.local.SecurePreferencesManager
import com.mayuri.ai.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MayuriVoiceViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = SecurePreferencesManager(application)
    private val db = MayuriDatabase.getInstance(application)
    private val aiRepository = AiRepository(prefs)
    val actionHandler = DeviceActionHandler(application)

    private val _statusText = MutableStateFlow("Tap the microphone or say 'Mayuri'")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _pendingCallIntent = MutableStateFlow<Intent?>(null)
    val pendingCallIntent: StateFlow<Intent?> = _pendingCallIntent.asStateFlow()

    val voiceManager = VoiceManager(application) { query ->
        handleUserSpeech(query)
    }

    val voiceState: StateFlow<VoiceState> = voiceManager.voiceState
    val rmsLevel: StateFlow<Float> = voiceManager.rmsLevel

    fun toggleListening() {
        if (voiceState.value is VoiceState.Listening) {
            voiceManager.stopListening()
            _statusText.value = "Paused."
        } else {
            voiceManager.startListening()
            _statusText.value = "Listening to you..."
        }
    }

    private fun handleUserSpeech(userInput: String) {
        viewModelScope.launch {
            _statusText.value = "Mayuri is thinking..."
            db.chatDao().insertMessage(ChatEntity(sender = "USER", content = userInput))

            val result = aiRepository.processQuery(userInput)
            result.onSuccess { intent ->
                executeIntent(intent.actionType, intent.target, intent.reply)
            }.onFailure { error ->
                val fallback = "Maaf kijiye, error aaya: ${error.localizedMessage}"
                _statusText.value = fallback
                voiceManager.speak(fallback)
            }
        }
    }

    private fun executeIntent(actionType: String, target: String?, reply: String) {
        // Speak response aloud to the user
        voiceManager.speak(reply, prefs.getSpeechPitch(), prefs.getSpeechSpeed())
        _statusText.value = reply

        viewModelScope.launch {
            db.chatDao().insertMessage(ChatEntity(sender = "MAYURI", content = reply))
        }

        // Execute corresponding native Android action
        when (actionType) {
            "DEVICE_CONTROL" -> {
                when (target?.lowercase()) {
                    "flashlight_on" -> actionHandler.toggleFlashlight(true)
                    "flashlight_off" -> actionHandler.toggleFlashlight(false)
                    "volume_up" -> actionHandler.adjustVolume(true)
                    "volume_down" -> actionHandler.adjustVolume(false)
                    "bluetooth" -> actionHandler.openSettings("bluetooth")
                    "wifi" -> actionHandler.openSettings("wifi")
                }
            }
            "OPEN_APP" -> target?.let { actionHandler.openApplication(it) }
            "CALL" -> target?.let {
                val callRes = actionHandler.initiateCall(it) { callIntent ->
                    _pendingCallIntent.value = callIntent
                }
                if (callRes is ActionResponse.NeedConfirmation) {
                    voiceManager.speak(callRes.message)
                }
            }
            "SOCIAL" -> target?.let {
                actionHandler.shareSocialContent(reply, it)
            }
        }
    }

    fun clearPendingCall() {
        _pendingCallIntent.value = null
    }

    suspend fun generateCode(prompt: String, lang: String): Result<String> {
        return aiRepository.generateCode(prompt, lang)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}