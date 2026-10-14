package com.example.model

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent

enum class PressureCurve(val id: String, val displayName: String, val exponent: Float) {
    LINEAR("linear", "Linear (Natural)", 1.0f),
    SOFT("soft", "Soft (Sensitive / Light Touch)", 0.7f),
    FIRM("firm", "Firm (Heavy / Hard Press)", 1.5f)
}

enum class TabletAction(val id: String, val displayName: String) {
    UNDO("undo", "Undo"),
    REDO("redo", "Redo"),
    PEN("pen", "Switch to Pen"),
    AREA_ERASER("area_eraser", "Switch to Area Eraser"),
    STROKE_ERASER("stroke_eraser", "Switch to Stroke Eraser"),
    HAND("hand", "Hand / Pan Tool"),
    LASSO("lasso", "Selection / Lasso"),
    TOGGLE_FULLSCREEN("toggle_fullscreen", "Toggle Full Screen"),
    TOGGLE_TOOLBAR("toggle_toolbar", "Hide/Show Toolbar"),
    ZOOM_IN("zoom_in", "Zoom In"),
    ZOOM_OUT("zoom_out", "Zoom Out"),
    RESET_ZOOM("reset_zoom", "Reset View (100%)"),
    CLEAR_PAGE("clear_page", "Clear Page"),
    NEW_PAGE("new_page", "New Page"),
    DELETE_PAGE("delete_page", "Delete Page"),
    PREV_PAGE("prev_page", "Previous Page"),
    NEXT_PAGE("next_page", "Next Page"),
    NONE("none", "None / Disabled")
}

data class TabletDeviceInfo(
    val id: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val descriptor: String,
    val sources: Int,
    val isExternal: Boolean
) {
    val vendorHex: String get() = "0x" + Integer.toHexString(vendorId).padStart(4, '0').uppercase()
    val productHex: String get() = "0x" + Integer.toHexString(productId).padStart(4, '0').uppercase()
    val sourceNames: List<String> get() {
        val list = mutableListOf<String>()
        if (sources and InputDevice.SOURCE_STYLUS != 0) list.add("STYLUS")
        if (sources and InputDevice.SOURCE_BLUETOOTH_STYLUS != 0) list.add("BT_STYLUS")
        if (sources and InputDevice.SOURCE_KEYBOARD != 0) list.add("KEYBOARD")
        if (sources and InputDevice.SOURCE_MOUSE != 0) list.add("MOUSE")
        if (sources and InputDevice.SOURCE_TOUCHSCREEN != 0) list.add("TOUCHSCREEN")
        if (sources and InputDevice.SOURCE_JOYSTICK != 0) list.add("JOYSTICK")
        return list
    }
}

data class HardwareInputDebugEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val deviceName: String,
    val deviceId: Int = 0,
    val vendorId: Int = 0,
    val productId: Int = 0,
    val keyCode: Int = 0,
    val keyName: String = "",
    val scanCode: Int = 0,
    val source: String = "",
    val buttonState: Int = 0,
    val buttonStateNames: List<String> = emptyList(),
    val toolType: String = "",
    val pressure: Float = 0f,
    val resolvedAction: String = ""
) {
    val vendorHex: String get() = "0x" + Integer.toHexString(vendorId).padStart(4, '0').uppercase()
    val productHex: String get() = "0x" + Integer.toHexString(productId).padStart(4, '0').uppercase()
}

data class TabletConfig(
    val pressureEnabled: Boolean = true,
    val pressureCurve: PressureCurve = PressureCurve.LINEAR,
    val minPressure: Float = 0.05f,
    val maxPressure: Float = 0.95f,
    val stabilizationEnabled: Boolean = true,
    val stylusOnlyDrawing: Boolean = false,
    val swapBarrelButtons: Boolean = false,
    val lowerBarrelAction: TabletAction = TabletAction.AREA_ERASER,
    val upperBarrelAction: TabletAction = TabletAction.STROKE_ERASER,
    val expressKeyMappings: Map<Int, TabletAction> = defaultExpressKeyMappings(),
    val customKeyCodes: Map<Int, Int> = emptyMap(),
    val customScanCodes: Map<Int, Int> = emptyMap()
) {
    // Backward compatibility accessor
    val barrelButtonAction: TabletAction get() = lowerBarrelAction

    fun matchesExpressKey(keyIndex: Int, keyCode: Int, scanCode: Int, event: KeyEvent? = null): Boolean {
        // 1. Check custom user binding (highest priority)
        val cKey = customKeyCodes[keyIndex]
        if (cKey != null && cKey != 0 && cKey == keyCode) return true

        val cScan = customScanCodes[keyIndex]
        if (cScan != null && cScan != 0 && cScan == scanCode) return true

        // 2. Standard Android BUTTON_1..8 (188..195)
        val standardBtn = when (keyIndex) {
            1 -> KeyEvent.KEYCODE_BUTTON_1
            2 -> KeyEvent.KEYCODE_BUTTON_2
            3 -> KeyEvent.KEYCODE_BUTTON_3
            4 -> KeyEvent.KEYCODE_BUTTON_4
            5 -> KeyEvent.KEYCODE_BUTTON_5
            6 -> KeyEvent.KEYCODE_BUTTON_6
            7 -> KeyEvent.KEYCODE_BUTTON_7
            8 -> KeyEvent.KEYCODE_BUTTON_8
            else -> -1
        }
        if (keyCode == standardBtn && standardBtn != -1) return true

        // 3. Gamepad keys A..R1 (Standard Android mapping for 8-button controllers & pads)
        val gamepadCode = when (keyIndex) {
            1 -> KeyEvent.KEYCODE_BUTTON_A // 96
            2 -> KeyEvent.KEYCODE_BUTTON_B // 97
            3 -> KeyEvent.KEYCODE_BUTTON_C // 98
            4 -> KeyEvent.KEYCODE_BUTTON_X // 99
            5 -> KeyEvent.KEYCODE_BUTTON_Y // 100
            6 -> KeyEvent.KEYCODE_BUTTON_Z // 101
            7 -> KeyEvent.KEYCODE_BUTTON_L1 // 102
            8 -> KeyEvent.KEYCODE_BUTTON_R1 // 103
            else -> -1
        }
        if (keyCode == gamepadCode && gamepadCode != -1) return true

        // Alternative Gamepad layout (4 face buttons + 4 shoulder buttons)
        val gamepadAlt = when (keyIndex) {
            5 -> KeyEvent.KEYCODE_BUTTON_L1 // 102
            6 -> KeyEvent.KEYCODE_BUTTON_R1 // 103
            7 -> KeyEvent.KEYCODE_BUTTON_L2 // 104
            8 -> KeyEvent.KEYCODE_BUTTON_R2 // 105
            else -> -1
        }
        if (keyCode == gamepadAlt && gamepadAlt != -1) return true

        // 4. Function keys F1..F8
        val fCode = when (keyIndex) {
            1 -> KeyEvent.KEYCODE_F1
            2 -> KeyEvent.KEYCODE_F2
            3 -> KeyEvent.KEYCODE_F3
            4 -> KeyEvent.KEYCODE_F4
            5 -> KeyEvent.KEYCODE_F5
            6 -> KeyEvent.KEYCODE_F6
            7 -> KeyEvent.KEYCODE_F7
            8 -> KeyEvent.KEYCODE_F8
            else -> -1
        }
        if (keyCode == fCode && fCode != -1) return true

        // 5. Numeric keys 1..8 and Numpad 1..8
        val numCode = when (keyIndex) {
            1 -> KeyEvent.KEYCODE_1
            2 -> KeyEvent.KEYCODE_2
            3 -> KeyEvent.KEYCODE_3
            4 -> KeyEvent.KEYCODE_4
            5 -> KeyEvent.KEYCODE_5
            6 -> KeyEvent.KEYCODE_6
            7 -> KeyEvent.KEYCODE_7
            8 -> KeyEvent.KEYCODE_8
            else -> -1
        }
        if (keyCode == numCode && numCode != -1) return true

        val numpadCode = when (keyIndex) {
            1 -> KeyEvent.KEYCODE_NUMPAD_1
            2 -> KeyEvent.KEYCODE_NUMPAD_2
            3 -> KeyEvent.KEYCODE_NUMPAD_3
            4 -> KeyEvent.KEYCODE_NUMPAD_4
            5 -> KeyEvent.KEYCODE_NUMPAD_5
            6 -> KeyEvent.KEYCODE_NUMPAD_6
            7 -> KeyEvent.KEYCODE_NUMPAD_7
            8 -> KeyEvent.KEYCODE_NUMPAD_8
            else -> -1
        }
        if (keyCode == numpadCode && numpadCode != -1) return true

        // 6. Linux evdev scan codes commonly sent by XP-PEN / UC-Logic kernel drivers
        // Range A: 256..263 (0x100..0x107: BTN_0..BTN_7)
        if (scanCode == 255 + keyIndex) return true
        // Range B: 288..295 (BTN_TRIGGER..)
        if (scanCode == 287 + keyIndex) return true
        // Range C: 294..301 (BTN_BASE..)
        if (scanCode == 293 + keyIndex) return true
        // Range D: 304..311 (BTN_SOUTH..BTN_TR)
        if (scanCode == 303 + keyIndex) return true
        // Range E: 704..711 (0x2C0..0x2C7: uclogic custom express keys)
        if (scanCode == 703 + keyIndex) return true

        // 7. XP-PEN Deco Mini 7 V2 / Deco Series Default Firmware Keyboard Shortcuts
        val isCtrl = event?.isCtrlPressed == true
        val isShift = event?.isShiftPressed == true
        val isAlt = event?.isAltPressed == true

        when (keyIndex) {
            1 -> {
                // K1: Undo (Ctrl+Z without Shift) or plain Z
                if ((isCtrl && !isShift && keyCode == KeyEvent.KEYCODE_Z) ||
                    (!isCtrl && !isAlt && !isShift && keyCode == KeyEvent.KEYCODE_Z)) return true
            }
            2 -> {
                // K2: Redo (Ctrl+Shift+Z or Ctrl+Y or plain Y)
                if ((isCtrl && isShift && keyCode == KeyEvent.KEYCODE_Z) ||
                    (isCtrl && keyCode == KeyEvent.KEYCODE_Y) ||
                    (!isCtrl && !isAlt && keyCode == KeyEvent.KEYCODE_Y)) return true
            }
            3 -> {
                // K3: Pan / Hand Tool (Space or H)
                if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_H) return true
            }
            4 -> {
                // K4: Save / Quick Tool (Ctrl+S, plain S)
                // Note: XP-PEN firmware assigns Save (S) to K4 in the top-4 cluster
                if ((isCtrl && keyCode == KeyEvent.KEYCODE_S) ||
                    (!isCtrl && !isAlt && keyCode == KeyEvent.KEYCODE_S)) return true
            }
            5 -> {
                // K5: Brush / Pen (B or P)
                if (keyCode == KeyEvent.KEYCODE_B || keyCode == KeyEvent.KEYCODE_P) return true
            }
            6 -> {
                // K6: Eraser / Brush Size Down / Zoom Out (E, [, Minus)
                if (keyCode == KeyEvent.KEYCODE_E ||
                    keyCode == KeyEvent.KEYCODE_LEFT_BRACKET ||
                    keyCode == KeyEvent.KEYCODE_MINUS ||
                    keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT) return true
            }
            7 -> {
                // K7: Zoom In / Brush Size Up (], Plus/Equals, Numpad Add)
                if (keyCode == KeyEvent.KEYCODE_RIGHT_BRACKET ||
                    keyCode == KeyEvent.KEYCODE_EQUALS ||
                    keyCode == KeyEvent.KEYCODE_PLUS ||
                    keyCode == KeyEvent.KEYCODE_NUMPAD_ADD) return true
            }
            8 -> {
                // K8: Zoom Out / Fullscreen / Shift / Move (Ctrl+Minus, Tab, F11, V)
                if ((isCtrl && (keyCode == KeyEvent.KEYCODE_MINUS || keyCode == KeyEvent.KEYCODE_NUMPAD_SUBTRACT)) ||
                    keyCode == KeyEvent.KEYCODE_TAB ||
                    keyCode == KeyEvent.KEYCODE_F11 ||
                    keyCode == KeyEvent.KEYCODE_V) return true
            }
        }

        return false
    }

    fun findMatchingKeyIndex(keyCode: Int, scanCode: Int, event: KeyEvent? = null): Int? {
        for (i in 1..8) {
            if (matchesExpressKey(i, keyCode, scanCode, event)) return i
        }
        return null
    }

    companion object {
        private const val PREFS_NAME = "inkkey_tablet_prefs"

        fun defaultExpressKeyMappings(): Map<Int, TabletAction> {
            return mapOf(
                1 to TabletAction.UNDO,
                2 to TabletAction.REDO,
                3 to TabletAction.HAND,
                4 to TabletAction.PEN,
                5 to TabletAction.AREA_ERASER,
                6 to TabletAction.STROKE_ERASER,
                7 to TabletAction.ZOOM_IN,
                8 to TabletAction.ZOOM_OUT
            )
        }

        fun load(context: Context): TabletConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pEnabled = prefs.getBoolean("pressure_enabled", true)
            val pCurveId = prefs.getString("pressure_curve", PressureCurve.LINEAR.id)
            val pCurve = PressureCurve.values().find { it.id == pCurveId } ?: PressureCurve.LINEAR
            val minP = prefs.getFloat("min_pressure", 0.05f)
            val maxP = prefs.getFloat("max_pressure", 0.95f)
            val stab = prefs.getBoolean("stabilization_enabled", true)
            val stylusOnly = prefs.getBoolean("stylus_only_drawing", false)
            val swapBarrel = prefs.getBoolean("swap_barrel_buttons", false)

            // Lower barrel button
            val lowerActionId = prefs.getString("lower_barrel_action", prefs.getString("barrel_action", TabletAction.AREA_ERASER.id))
            val lowerAction = TabletAction.values().find { it.id == lowerActionId } ?: TabletAction.AREA_ERASER

            // Upper barrel button
            val upperActionId = prefs.getString("upper_barrel_action", TabletAction.STROKE_ERASER.id)
            val upperAction = TabletAction.values().find { it.id == upperActionId } ?: TabletAction.STROKE_ERASER

            val keyMap = mutableMapOf<Int, TabletAction>()
            val customKeys = mutableMapOf<Int, Int>()
            val customScans = mutableMapOf<Int, Int>()

            val defaults = defaultExpressKeyMappings()
            for (i in 1..8) {
                val defaultAction = defaults[i] ?: TabletAction.NONE
                val actionId = prefs.getString("express_key_$i", defaultAction.id)
                val action = TabletAction.values().find { it.id == actionId } ?: defaultAction
                keyMap[i] = action

                val cKey = prefs.getInt("custom_key_$i", 0)
                if (cKey != 0) customKeys[i] = cKey

                val cScan = prefs.getInt("custom_scan_$i", 0)
                if (cScan != 0) customScans[i] = cScan
            }

            return TabletConfig(
                pressureEnabled = pEnabled,
                pressureCurve = pCurve,
                minPressure = minP,
                maxPressure = maxP,
                stabilizationEnabled = stab,
                stylusOnlyDrawing = stylusOnly,
                swapBarrelButtons = swapBarrel,
                lowerBarrelAction = lowerAction,
                upperBarrelAction = upperAction,
                expressKeyMappings = keyMap,
                customKeyCodes = customKeys,
                customScanCodes = customScans
            )
        }

        fun save(context: Context, config: TabletConfig) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
                .putBoolean("pressure_enabled", config.pressureEnabled)
                .putString("pressure_curve", config.pressureCurve.id)
                .putFloat("min_pressure", config.minPressure)
                .putFloat("max_pressure", config.maxPressure)
                .putBoolean("stabilization_enabled", config.stabilizationEnabled)
                .putBoolean("stylus_only_drawing", config.stylusOnlyDrawing)
                .putBoolean("swap_barrel_buttons", config.swapBarrelButtons)
                .putString("lower_barrel_action", config.lowerBarrelAction.id)
                .putString("barrel_action", config.lowerBarrelAction.id) // legacy fallback
                .putString("upper_barrel_action", config.upperBarrelAction.id)

            for (i in 1..8) {
                val action = config.expressKeyMappings[i] ?: TabletAction.NONE
                editor.putString("express_key_$i", action.id)

                val cKey = config.customKeyCodes[i] ?: 0
                editor.putInt("custom_key_$i", cKey)

                val cScan = config.customScanCodes[i] ?: 0
                editor.putInt("custom_scan_$i", cScan)
            }
            editor.apply()
        }

        /**
         * Detects connected physical drawing tablets, digitizers, and styluses (e.g. XP-PEN Deco Mini 7).
         */
        fun getConnectedTabletDevices(context: Context): List<String> {
            return getConnectedTabletDevicesDetailed(context).map { it.name }
        }

        fun getConnectedTabletDevicesDetailed(context: Context): List<TabletDeviceInfo> {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager ?: return emptyList()
            val list = mutableListOf<TabletDeviceInfo>()
            val ids = inputManager.inputDeviceIds ?: return emptyList()
            for (id in ids) {
                val device = inputManager.getInputDevice(id) ?: continue
                val sources = device.sources
                val vId = device.vendorId
                val isStylusOrTablet = (sources and InputDevice.SOURCE_STYLUS != 0) ||
                        (sources and InputDevice.SOURCE_BLUETOOTH_STYLUS != 0) ||
                        vId == 0x28BD || // XP-Pen
                        vId == 0x5543 || // UC-Logic
                        vId == 0x256C || // Huion
                        vId == 0x056A || // Wacom
                        device.name.contains("XP-PEN", ignoreCase = true) ||
                        device.name.contains("Deco", ignoreCase = true) ||
                        device.name.contains("Tablet", ignoreCase = true) ||
                        device.name.contains("Digitizer", ignoreCase = true) ||
                        device.name.contains("UG-", ignoreCase = true) ||
                        device.name.contains("Huion", ignoreCase = true) ||
                        device.name.contains("Wacom", ignoreCase = true)
                if (isStylusOrTablet) {
                    list.add(
                        TabletDeviceInfo(
                            id = device.id,
                            name = device.name,
                            vendorId = device.vendorId,
                            productId = device.productId,
                            descriptor = device.descriptor ?: "",
                            sources = device.sources,
                            isExternal = !device.isVirtual
                        )
                    )
                }
            }
            return list
        }
    }
}
