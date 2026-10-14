package com.example.util

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import java.util.Locale

/** Reconstructed from the verified final clean APK. */
object StylusHardwareDetector {
    enum class BarrelTarget { NONE, LOWER_BARREL, UPPER_BARREL }
    fun classifyScanCode(scanCode: Int): BarrelTarget = when (scanCode) {
        273, 288, 308, 331 -> BarrelTarget.LOWER_BARREL
        274, 289, 290, 309, 310, 311, 332 -> BarrelTarget.UPPER_BARREL
        else -> BarrelTarget.NONE
    }

    fun classifyMotionEvent(event: MotionEvent): BarrelTarget {
        if (event.pointerCount > 0 && event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER)
            return BarrelTarget.LOWER_BARREL
        val upperMask = MotionEvent.BUTTON_STYLUS_SECONDARY or MotionEvent.BUTTON_SECONDARY or
            MotionEvent.BUTTON_TERTIARY or MotionEvent.BUTTON_FORWARD or MotionEvent.BUTTON_BACK
        if (event.buttonState and upperMask != 0 || event.actionButton in intArrayOf(64, 4, 2, 16, 8))
            return BarrelTarget.UPPER_BARREL
        if (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0 || event.actionButton == 32)
            return BarrelTarget.LOWER_BARREL
        return BarrelTarget.NONE
    }

    fun classifyKeyEvent(keyCode: Int, scanCode: Int, event: KeyEvent? = null): BarrelTarget {
        classifyScanCode(scanCode).let { if (it != BarrelTarget.NONE) return it }
        return when (keyCode) {
            100, 288, 308 -> BarrelTarget.LOWER_BARREL
            101, 289, 290, 309, 310, 311 -> BarrelTarget.UPPER_BARREL
            KeyEvent.KEYCODE_E -> {
                val dev = event?.device
                val name = dev?.name?.lowercase(Locale.ROOT).orEmpty()
                val external = dev != null && !dev.isVirtual
                val stylusOrPad = dev != null &&
                    (dev.sources and InputDevice.SOURCE_STYLUS != 0 || dev.sources and InputDevice.SOURCE_GAMEPAD != 0)
                if (event?.isCtrlPressed != true && event?.isAltPressed != true &&
                    (external || stylusOrPad) && !name.contains("ugtablet deco mini7 v2"))
                    BarrelTarget.UPPER_BARREL else BarrelTarget.NONE
            }
            else -> BarrelTarget.NONE
        }
    }
}
