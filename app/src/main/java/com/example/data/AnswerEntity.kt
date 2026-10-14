package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AnswerDocument
import com.example.model.CanvasScrollMode
import com.example.model.NeetSubject
import com.example.model.PaperStyle

@Entity(tableName = "answers")
data class AnswerEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val questionNumber: String,
    val paperStyle: String,
    val scrollMode: String,
    val pagesJson: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDocument(): AnswerDocument {
        val parsedSubject = try { NeetSubject.valueOf(subject) } catch (e: Exception) { NeetSubject.GENERAL }
        val parsedPaper = try { PaperStyle.valueOf(paperStyle) } catch (e: Exception) { PaperStyle.RULED }
        val parsedScroll = try { CanvasScrollMode.valueOf(scrollMode) } catch (e: Exception) { CanvasScrollMode.VERTICAL }
        val parsedPages = if (pagesJson.isBlank()) listOf(com.example.model.PageData(0)) else StrokesSerializer.deserializePages(pagesJson)

        return AnswerDocument(
            id = id,
            title = title,
            subject = parsedSubject,
            questionNumber = questionNumber,
            paperStyle = parsedPaper,
            scrollMode = parsedScroll,
            pages = parsedPages,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDocument(doc: AnswerDocument): AnswerEntity {
            return AnswerEntity(
                id = doc.id,
                title = doc.title,
                subject = doc.subject.name,
                questionNumber = doc.questionNumber,
                paperStyle = doc.paperStyle.name,
                scrollMode = doc.scrollMode.name,
                pagesJson = StrokesSerializer.serializePages(doc.pages),
                createdAt = doc.createdAt,
                updatedAt = doc.updatedAt
            )
        }
    }
}
