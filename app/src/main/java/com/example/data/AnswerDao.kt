package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    @Query("SELECT id, title, subject, questionNumber, paperStyle, scrollMode, '' AS pagesJson, createdAt, updatedAt FROM answers ORDER BY updatedAt DESC")
    fun getAllAnswerSummaries(): Flow<List<AnswerEntity>>

    @Query("SELECT * FROM answers ORDER BY updatedAt DESC")
    fun getAllAnswers(): Flow<List<AnswerEntity>>

    @Query("SELECT * FROM answers WHERE id = :id LIMIT 1")
    suspend fun getAnswerById(id: String): AnswerEntity?

    @Query("SELECT * FROM answers ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestAnswer(): AnswerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswer(answer: AnswerEntity)

    @Query("DELETE FROM answers WHERE id = :id")
    suspend fun deleteAnswerById(id: String)

    @Query("DELETE FROM answers")
    suspend fun deleteAllAnswers()
}
