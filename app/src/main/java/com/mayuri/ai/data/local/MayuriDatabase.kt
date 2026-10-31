package com.mayuri.ai.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER" or "MAYURI"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val conversationTag: String = "DEFAULT" // Categorizes chats (e.g. "CODE", "VOICE")
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationTag = :tag ORDER BY timestamp ASC")
    fun getMessages(tag: String = "DEFAULT"): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatEntity)

    @Query("DELETE FROM chat_messages WHERE conversationTag = :tag")
    suspend fun clearHistory(tag: String = "DEFAULT")
}

@Database(entities = [ChatEntity::class], version = 1, exportSchema = false)
abstract class MayuriDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: MayuriDatabase? = null

        fun getInstance(context: android.content.Context): MayuriDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MayuriDatabase::class.java,
                    "mayuri_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}