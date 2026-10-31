package com.mayuri.ai.core.voice

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Wraps Android's SpeechRecognizer (input) and TextToSpeech (output).
 * Emits [VoiceState] and a normalised microphone RMS level for the UI.
 */
class VoiceManager(
    private val app: Application,
    private val onSpeechResult: (String) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = TextToSpeech(app, this)

    init {
        if (SpeechRecognizer.isRecognitionAvailable(app)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(app).also {
                it.setRecognitionListener(this)
            }
        }
    }

    fun startListening() {
        val recognizer = speechRecognizer ?: run {
            _voiceState.value = VoiceState.Idle
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        _voiceState.value = VoiceState.Listening
        recognizer.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _voiceState.value = VoiceState.Idle
        _rmsLevel.value = 0f
    }

    fun speak(text: String, pitch: Float = 1.0f, speed: Float = 1.0f) {
        val engine = tts ?: return
        engine.setPitch(pitch)
        engine.setSpeechRate(speed)
        _voiceState.value = VoiceState.Speaking
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mayuri-utterance")
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("hi", "IN")
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = VoiceState.Listening
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) {
        _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
    }

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        _rmsLevel.value = 0f
    }

    override fun onError(error: Int) {
        _voiceState.value = VoiceState.Idle
        _rmsLevel.value = 0f
    }

    override fun onResults(results: Bundle?) {
        val spoken = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
        _rmsLevel.value = 0f
        if (spoken.isNullOrBlank()) {
            _voiceState.value = VoiceState.Idle
        } else {
            _voiceState.value = VoiceState.Thinking
            onSpeechResult(spoken)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
