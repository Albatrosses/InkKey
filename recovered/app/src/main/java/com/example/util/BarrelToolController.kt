package com.example.util

import com.example.model.TabletAction
import com.example.model.ToolType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps held tools separate from the user's selected toolbar tool. */
class BarrelToolController(
    private val actionFor: (Boolean) -> TabletAction,
    private val execute: (TabletAction) -> Unit
) {
    enum class Input { LOWER, UPPER, INFERRED_UPPER, KEY_LOWER, KEY_UPPER }
    private fun isUpper(input: Input) = input != Input.LOWER && input != Input.KEY_LOWER
    private val held = mutableSetOf<Input>()
    private val _tool = MutableStateFlow<ToolType?>(null)
    val tool = _tool.asStateFlow()
    private val _active = MutableStateFlow(false)
    val active = _active.asStateFlow()

    fun setPressed(input: Input, pressed: Boolean) {
        val alreadyPressed = held.any { isUpper(it) == isUpper(input) }
        val changed = if (pressed) held.add(input) else held.remove(input)
        if (changed && pressed && !alreadyPressed) {
            val action = actionFor(isUpper(input))
            if (action != TabletAction.NONE && toolFor(action) == null) execute(action)
        }
        refresh()
    }

    fun refresh() {
        val input = when {
            Input.LOWER in held -> Input.LOWER
            Input.KEY_LOWER in held -> Input.KEY_LOWER
            Input.UPPER in held -> Input.UPPER
            Input.KEY_UPPER in held -> Input.KEY_UPPER
            Input.INFERRED_UPPER in held -> Input.INFERRED_UPPER
            else -> null
        }
        _active.value = input != null
        _tool.value = input?.let { toolFor(actionFor(isUpper(it))) }
    }

    fun clear() { held.clear(); refresh() }

    companion object {
        fun toolFor(action: TabletAction): ToolType? = when (action) {
            TabletAction.PEN -> ToolType.PEN
            TabletAction.AREA_ERASER -> ToolType.AREA_ERASER
            TabletAction.STROKE_ERASER -> ToolType.STROKE_ERASER
            TabletAction.HAND -> ToolType.HAND
            TabletAction.LASSO -> ToolType.LASSO
            else -> null
        }
    }
}
