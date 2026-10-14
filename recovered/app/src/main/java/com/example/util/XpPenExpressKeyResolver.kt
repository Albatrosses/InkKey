package com.example.util

import android.view.KeyEvent

/**
 * Exact XP-Pen Deco Mini7 V2 express-key identity recovered from hardware
 * captures in the InkKey debugging sessions.
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

    /**
     * Recovered physical layout:
     * K1 = B
     * K2 = E
     * K3 = Left Alt
     * K4 = Space
     * K5 = Ctrl+S
     * K6 = Ctrl+Z
     * K7 = scan code 78 / KEYCODE_NUMPAD_ADD
     * K8 = scan code 74 / KEYCODE_NUMPAD_SUBTRACT
     */
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
        else -> null
    }

    /** Hardware scan codes captured directly from the Deco Mini7 V2 monitor. */
    fun resolveCapturedScanCode(scanCode: Int): Int? = when (scanCode) {
        78 -> 7
        74 -> 8
        else -> null
    }

    fun resolve(event: KeyEvent): Int? {
        if (!isXpPen(event)) return null
        return resolveCapturedScanCode(event.scanCode) ?: resolveKnownShortcut(
            keyCode = event.keyCode,
            ctrl = event.isCtrlPressed,
            alt = event.isAltPressed,
            shift = event.isShiftPressed
        )
    }

    /** Modifier prefix packets used by composite express keys such as Ctrl+S/Z. */
    fun isCompositePrefix(event: KeyEvent): Boolean = isXpPen(event) && when (event.keyCode) {
        KeyEvent.KEYCODE_CTRL_LEFT,
        KeyEvent.KEYCODE_CTRL_RIGHT,
        KeyEvent.KEYCODE_SHIFT_LEFT,
        KeyEvent.KEYCODE_SHIFT_RIGHT,
        KeyEvent.KEYCODE_ALT_RIGHT -> true
        // Left Alt is K3 itself, so never treat it as a disposable prefix.
        else -> false
    }

    fun chordLabel(index: Int): String = when (index) {
        1 -> "B"
        2 -> "E"
        3 -> "Left Alt"
        4 -> "Space"
        5 -> "Ctrl+S"
        6 -> "Ctrl+Z"
        7 -> "scan 78"
        8 -> "scan 74"
        else -> "Learned"
    }
}
