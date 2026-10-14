package com.example.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

/** Gives the whole toolbar a hit target, including gaps between its buttons. */
fun Modifier.symbolControlGuard(): Modifier = zIndex(10f).pointerInput(Unit) {
    detectTapGestures(onTap = {})
}
