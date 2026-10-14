package com.example.util

import android.view.KeyEvent

/**
 * Resolver for the known XP-Pen Deco Mini7 V2 express-key shortcuts recovered
 * during InkKey debugging. Keeping the mapping in one place prevents the IME
 * and the main Activity from drifting apart again.
 */
object XpPenExpressKeyResolver {
    fun isXpPen(event: KeyEvent?): Boolean {
        val dev = event?.device ?: return false
        val name = dev.name.orEmpty().lowercase(java.util.Locale.ROOT)
        return name.contains("ugtablet deco mini7 v2") ||
            name.contains("xp-pen deco mini7") ||
            name.contains("deco mini7 v2") ||
            dev.vendorId == 0x28BD
    }

    /** Pure shortcut resolver so the recovered K1-K8 layout is unit-testable. */
    fun resolveKnownShortcut(
        keyCode: Int,
        ctrl: Boolean,
        alt: Boolean,
        shift: Boolean
    ): Int? = when {
        !ctrl && !alt && !shift && keyCode == KeyEvent.KEYCODE_B -> 1
        !ctrl && !alt && !shift && keyCode == KeyEvent.KEYCODE_E -> 2
        !ctrl && !shift && keyCode == KeyEvent.KEYCODE_ALT_LEFT -> 3
        !ctrl && !alt && !shift && keyCode == KeyEvent.KEYCODE_SPACE -> 4
        ctrl && !alt && keyCode == KeyEvent.KEYCODE_S -> 5
        ctrl && !alt && keyCode == KeyEvent.KEYCODE_Z -> 6
        ctrl && keyCode == KeyEvent.KEYCODE_NUMPAD_ADD -> 7
        ctrl && keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> 8
        else -> null
    }

    fun resolve(event: KeyEvent): Int? {
        if (!isXpPen(event)) return null
        return resolveKnownShortcut(
            keyCode = event.keyCode,
            ctrl = event.isCtrlPressed,
            alt = event.isAltPressed,
            shift = event.isShiftPressed
        )
    }
}
