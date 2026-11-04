package com.aether.agent.data.local.db
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao interface ChatDao {
 @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY createdAt ASC") fun observeMessages(sessionId:String):Flow<List<MessageEntity>>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun insert(message:MessageEntity)
 @Query("DELETE FROM messages WHERE sessionId = :sessionId") suspend fun clearSession(sessionId:String)
}
