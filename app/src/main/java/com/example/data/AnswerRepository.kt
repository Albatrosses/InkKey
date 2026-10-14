package com.example.data

import com.example.model.AnswerDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AnswerRepository(private val answerDao: AnswerDao) {

    val allAnswers: Flow<List<AnswerDocument>> = answerDao.getAllAnswerSummaries().map { entities ->
        entities.map { it.toDocument() }
    }

    suspend fun getLatestAnswer(): AnswerDocument? = withContext(Dispatchers.IO) {
        answerDao.getLatestAnswer()?.toDocument()
    }

    suspend fun getAnswerById(id: String): AnswerDocument? = withContext(Dispatchers.IO) {
        answerDao.getAnswerById(id)?.toDocument()
    }

    suspend fun saveAnswer(doc: AnswerDocument) = withContext(Dispatchers.IO) {
        val entity = AnswerEntity.fromDocument(doc.copy(updatedAt = System.currentTimeMillis()))
        answerDao.insertAnswer(entity)
    }

    suspend fun deleteAnswer(id: String) = withContext(Dispatchers.IO) {
        answerDao.deleteAnswerById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        answerDao.deleteAllAnswers()
    }
}
