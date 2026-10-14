package com.example.data

import com.example.model.DrawingPoint
import com.example.model.DrawingStroke
import com.example.model.PageData
import com.example.model.ShapeType
import com.example.model.TextAnnotation
import com.example.model.ToolType
import org.json.JSONArray
import org.json.JSONObject

object StrokesSerializer {

    fun serializePages(pages: List<PageData>): String {
        val rootArray = JSONArray()
        for (page in pages) {
            val pageObj = JSONObject()
            pageObj.put("pageIndex", page.pageIndex)

            val strokesArr = JSONArray()
            for (stroke in page.strokes) {
                val strokeObj = JSONObject()
                strokeObj.put("id", stroke.id)
                strokeObj.put("toolType", stroke.toolType.name)
                strokeObj.put("color", stroke.color)
                strokeObj.put("width", stroke.width.toDouble())
                strokeObj.put("alpha", stroke.alpha.toDouble())
                strokeObj.put("isShape", stroke.isShape)
                strokeObj.put("shapeType", stroke.shapeType.name)

                val pointsArr = JSONArray()
                for (p in stroke.points) {
                    val pArr = JSONArray()
                    pArr.put(p.x.toDouble())
                    pArr.put(p.y.toDouble())
                    pArr.put(p.pressure.toDouble())
                    pointsArr.put(pArr)
                }
                strokeObj.put("points", pointsArr)
                strokesArr.put(strokeObj)
            }
            pageObj.put("strokes", strokesArr)

            val textsArr = JSONArray()
            for (text in page.textAnnotations) {
                val textObj = JSONObject()
                textObj.put("id", text.id)
                textObj.put("text", text.text)
                textObj.put("x", text.x.toDouble())
                textObj.put("y", text.y.toDouble())
                textObj.put("fontSize", text.fontSize.toDouble())
                textObj.put("color", text.color)
                textObj.put("rotation", text.rotation.toDouble())
                textsArr.put(textObj)
            }
            pageObj.put("texts", textsArr)

            rootArray.put(pageObj)
        }
        return rootArray.toString()
    }

    fun deserializePages(json: String): List<PageData> {
        if (json.isBlank()) return listOf(PageData(0))
        return try {
            val rootArray = JSONArray(json)
            val pages = mutableListOf<PageData>()
            for (i in 0 until rootArray.length()) {
                val pageObj = rootArray.getJSONObject(i)
                val pageIndex = pageObj.optInt("pageIndex", i)

                val strokesList = mutableListOf<DrawingStroke>()
                val strokesArr = pageObj.optJSONArray("strokes") ?: JSONArray()
                for (j in 0 until strokesArr.length()) {
                    val strokeObj = strokesArr.getJSONObject(j)
                    val id = strokeObj.optString("id", java.util.UUID.randomUUID().toString())
                    val toolTypeName = strokeObj.optString("toolType", ToolType.PEN.name)
                    val toolType = try { ToolType.valueOf(toolTypeName) } catch (e: Exception) { ToolType.PEN }
                    val color = strokeObj.optLong("color", 0xFF1E293B)
                    val width = strokeObj.optDouble("width", 4.0).toFloat()
                    val alpha = strokeObj.optDouble("alpha", 1.0).toFloat()
                    val isShape = strokeObj.optBoolean("isShape", false)
                    val shapeTypeName = strokeObj.optString("shapeType", ShapeType.NONE.name)
                    val shapeType = try { ShapeType.valueOf(shapeTypeName) } catch (e: Exception) { ShapeType.NONE }

                    val pointsList = mutableListOf<DrawingPoint>()
                    val pointsArr = strokeObj.optJSONArray("points") ?: JSONArray()
                    for (k in 0 until pointsArr.length()) {
                        val pArr = pointsArr.getJSONArray(k)
                        val px = pArr.optDouble(0, 0.0).toFloat()
                        val py = pArr.optDouble(1, 0.0).toFloat()
                        val pPress = pArr.optDouble(2, 1.0).toFloat()
                        pointsList.add(DrawingPoint(px, py, pPress))
                    }

                    strokesList.add(
                        DrawingStroke(
                            id = id,
                            toolType = toolType,
                            color = color,
                            width = width,
                            alpha = alpha,
                            points = pointsList,
                            isShape = isShape,
                            shapeType = shapeType
                        )
                    )
                }

                val textsList = mutableListOf<TextAnnotation>()
                val textsArr = pageObj.optJSONArray("texts") ?: JSONArray()
                for (t in 0 until textsArr.length()) {
                    val textObj = textsArr.getJSONObject(t)
                    val tid = textObj.optString("id", java.util.UUID.randomUUID().toString())
                    val txt = textObj.optString("text", "")
                    val tx = textObj.optDouble("x", 0.0).toFloat()
                    val ty = textObj.optDouble("y", 0.0).toFloat()
                    val tFont = textObj.optDouble("fontSize", 16.0).toFloat()
                    val tColor = textObj.optLong("color", 0xFF1E293B)
                    val tRotation = textObj.optDouble("rotation", 0.0).toFloat()
                    textsList.add(TextAnnotation(tid, txt, tx, ty, tFont, tColor, isSelected = false, rotation = tRotation))

                }

                pages.add(PageData(pageIndex = pageIndex, strokes = strokesList, textAnnotations = textsList))
            }
            if (pages.isEmpty()) listOf(PageData(0)) else pages
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(PageData(0))
        }
    }
}
