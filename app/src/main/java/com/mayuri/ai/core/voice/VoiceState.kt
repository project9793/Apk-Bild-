package com.mayuri.ai.core.voice

/** High level cognitive state of the assistant, used to drive the UI. */
sealed class VoiceState {
    data object Idle : VoiceState()
    data object Listening : VoiceState()
    data object Thinking : VoiceState()
    data object Speaking : VoiceState()
}
