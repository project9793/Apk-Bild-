package com.mayuri.ai.data.local

/** One turn of the conversation history. */
data class ChatEntity(
    val id: Long = 0L,
    val sender: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
