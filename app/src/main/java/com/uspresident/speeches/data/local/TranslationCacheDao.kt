package com.uspresident.speeches.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "translation_cache")
data class TranslationCacheEntity(
    @PrimaryKey val sourceHash: String,
    val sourceText: String,
    val translatedText: String,
    val createdAt: Long,
)

@Dao
interface TranslationCacheDao {
    @Query("SELECT * FROM translation_cache WHERE sourceHash = :sourceHash LIMIT 1")
    suspend fun getByHash(sourceHash: String): TranslationCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranslationCacheEntity)
}
