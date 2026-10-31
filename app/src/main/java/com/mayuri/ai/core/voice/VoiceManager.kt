package com.mayuri.ai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    object Idle : VoiceState()
    object Listening : VoiceState()
    object Thinking : VoiceState()
    data class Speaking(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class VoiceManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    init {
        initSpeechRecognizer()
        textToSpeech = TextToSpeech(context, this)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceManager)
            }
        }
    }

    fun startListening() {
        stopSpeaking()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
        _voiceState.value = VoiceState.Listening
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        if (_voiceState.value is VoiceState.Listening) {
            _voiceState.value = VoiceState.Idle
        }
    }

    fun speak(text: String, pitch: Float = 1.15f, speed: Float = 1.05f) {
        if (!isTtsInitialized) return
        _voiceState.value = VoiceState.Speaking(text)
        
        textToSpeech?.apply {
            setPitch(pitch) // Higher pitch delivers a natural female tone
            setSpeechRate(speed)
            speak(text, TextToSpeech.QUEUE_FLUSH, null, "MAYURI_UTTERANCE_ID")
        }
    }

    fun stopSpeaking() {
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }
        _voiceState.value = VoiceState.Idle
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = Locale.ENGLISH
            }
            
            // Prefer an Indian English or Hindi female voice variant if available on device
            textToSpeech?.voices?.firstOrNull { voice ->
                voice.name.contains("female", ignoreCase = true) ||
                voice.name.contains("hi-in", ignoreCase = true)
            }?.let { matchedVoice ->
                textToSpeech?.voice = matchedVoice
            }

            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    _voiceState.value = VoiceState.Idle
                }
                override fun onError(utteranceId: String?) {
                    _voiceState.value = VoiceState.Idle
                }
            })
            isTtsInitialized = true
        }
    }

    // SpeechRecognizer Callbacks
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            _voiceState.value = VoiceState.Thinking
            onSpeechRecognized(text)
        } else {
            _voiceState.value = VoiceState.Idle
        }
    }

    override fun onRmsChanged(rmsdB: Float) {
        _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
    }

    override fun onError(error: Int) {
        _voiceState.value = VoiceState.Error("Error code: $error")
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}