package com.mayuri.ai.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Lightweight in-memory chat history store.
 *
 * This is intentionally NOT Room yet, so the project builds without KSP.
 * Swap the body for a Room database later without touching callers.
 */
class MayuriChatDao internal constructor() {

    private val _messages = MutableStateFlow<List<ChatEntity>>(emptyList())
    val messages: StateFlow<List<ChatEntity>> = _messages.asStateFlow()

    private var nextId = 1L

    suspend fun insertMessage(message: ChatEntity) {
        _messages.value = _messages.value + message.copy(id = nextId++)
    }

    suspend fun getAllMessages(): List<ChatEntity> = _messages.value

    suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}

class MayuriDatabase private constructor() {

    private val dao = MayuriChatDao()

    fun chatDao(): MayuriChatDao = dao

    companion object {
        @Volatile
        private var instance: MayuriDatabase? = null

        fun getInstance(context: Context): MayuriDatabase =
            instance ?: synchronized(this) {
                instance ?: MayuriDatabase().also { instance = it }
            }
    }
}
