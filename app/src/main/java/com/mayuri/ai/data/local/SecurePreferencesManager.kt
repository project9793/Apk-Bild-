package com.mayuri.ai.data.local

import android.content.Context
import androidx.core.content.edit

/**
 * Stores user settings and API keys.
 *
 * NOTE: this uses plain SharedPreferences, so values are private to the app
 * sandbox but NOT encrypted. Move to an encrypted store before shipping.
 */
class SecurePreferencesManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getGeminiApiKey(): String? =
        prefs.getString(KEY_GEMINI, null)?.takeIf { it.isNotBlank() }

    fun getOpenAiApiKey(): String? =
        prefs.getString(KEY_OPENAI, null)?.takeIf { it.isNotBlank() }

    fun saveGeminiApiKey(key: String) = prefs.edit { putString(KEY_GEMINI, key) }

    fun saveOpenAiApiKey(key: String) = prefs.edit { putString(KEY_OPENAI, key) }

    fun getProvider(): String = prefs.getString(KEY_PROVIDER, PROVIDER_GEMINI) ?: PROVIDER_GEMINI

    fun setProvider(provider: String) = prefs.edit { putString(KEY_PROVIDER, provider) }

    fun getSpeechPitch(): Float = prefs.getFloat(KEY_PITCH, 1.0f)

    fun setSpeechPitch(value: Float) = prefs.edit { putFloat(KEY_PITCH, value) }

    fun getSpeechSpeed(): Float = prefs.getFloat(KEY_SPEED, 1.0f)

    fun setSpeechSpeed(value: Float) = prefs.edit { putFloat(KEY_SPEED, value) }

    private companion object {
        const val PREFS_NAME = "mayuri_prefs"
        const val KEY_GEMINI = "gemini_api_key"
        const val KEY_OPENAI = "openai_api_key"
        const val KEY_PROVIDER = "ai_provider"
        const val KEY_PITCH = "speech_pitch"
        const val KEY_SPEED = "speech_speed"
        const val PROVIDER_GEMINI = "GEMINI"
    }
}
