package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TranslationItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {

    @Query("SELECT * FROM translations ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<TranslationItem>>

    @Query("SELECT * FROM translations WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<TranslationItem>>

    @Query("""
        SELECT * FROM translations 
        WHERE sourceText LIKE '%' || :query || '%' 
           OR translatedText LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchTranslations(query: String): Flow<List<TranslationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(item: TranslationItem): Long

    @Update
    suspend fun updateTranslation(item: TranslationItem)

    @Delete
    suspend fun deleteTranslation(item: TranslationItem)

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE translations SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM translations WHERE isFavorite = 0")
    suspend fun clearNonFavorites()

    @Query("DELETE FROM translations")
    suspend fun clearAll()
}
