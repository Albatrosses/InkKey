package com.example.util

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import com.example.model.TabletAction
import com.example.model.TabletConfig

/** Shared by the workspace and IME; reads the tool synchronously for each event. */
class BarrelInputRouter(config: () -> TabletConfig, execute: (TabletAction) -> Unit) {
    val tools = BarrelToolController({ upper ->
        val c = config()
        if (upper != c.swapBarrelButtons) c.upperBarrelAction else c.lowerBarrelAction
    }, execute)

    private val detector = ContactRestartDetector()
    private val handler = Handler(Looper.getMainLooper())
    private val leaveRange = Runnable { clearInferred() }

    /** Root-window and Compose handlers can see the same MotionEvent. Deduplicate without
     * throwing away later events that happen to share an eventTime. */
    private data class EventKey(
        val time: Long,
        val action: Int,
        val deviceId: Int,
        val buttons: Int,
        val actionButton: Int,
        val pressureBits: Int,
        val xBits: Int,
        val yBits: Int
    )

    private var lastTouchKey: EventKey? = null
    private var lastHoverKey: EventKey? = null
    private var lastHoverHandled = false

    private fun eventKey(event: MotionEvent): EventKey = EventKey(
        time = event.eventTime,
        action = event.actionMasked,
        deviceId = event.deviceId,
        buttons = event.buttonState,
        actionButton = try { event.actionButton } catch (_: Exception) { 0 },
        pressureBits = if (event.pointerCount > 0) java.lang.Float.floatToIntBits(event.pressure) else 0,
        xBits = if (event.pointerCount > 0) java.lang.Float.floatToIntBits(event.x) else 0,
        yBits = if (event.pointerCount > 0) java.lang.Float.floatToIntBits(event.y) else 0
    )

    private fun trackedStylus(event: MotionEvent): Boolean {
        val dev = event.device ?: return false
        return dev.vendorId == 0x28BD && dev.productId == 0x0948 &&
            event.pointerCount == 1 && event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS
    }

    private fun eligible(event: MotionEvent): Boolean = trackedStylus(event) &&
        event.buttonState == 0 && (try { event.actionButton } catch (_: Exception) { 0 }) == 0

    private fun edge(edge: ContactRestartDetector.Edge, time: Long) {
        if (edge == ContactRestartDetector.Edge.NONE) return
        tools.setPressed(
            BarrelToolController.Input.INFERRED_UPPER,
            edge == ContactRestartDetector.Edge.PRESS_CANDIDATE
        )
        UpperTrialTrace.record("INFERRED_EDGE=$edge time=$time override=${tools.tool.value}")
    }

    fun touch(event: MotionEvent) {
        val action = event.actionMasked
        if (action in intArrayOf(
                MotionEvent.ACTION_HOVER_ENTER,
                MotionEvent.ACTION_HOVER_MOVE,
                MotionEvent.ACTION_HOVER_EXIT
            )) return

        val k = eventKey(event)
        if (k == lastTouchKey) return
        lastTouchKey = k

        if (action == MotionEvent.ACTION_DOWN) handler.removeCallbacks(leaveRange)
        if (action == MotionEvent.ACTION_CANCEL) {
            reset()
            return
        }

        val hasPhysical = physical(event)

        // Trial 6 exposed an important state bug: finger/palm/other-device events must not
        // release an inferred stylus button that is still physically held.
        if (!trackedStylus(event)) return

        // Real Android barrel bits always win over inference and clear only inferred state.
        if (hasPhysical) {
            clearInferred()
            return
        }

        if (!eligible(event)) return
        UpperTrialTrace.motion(event, "TOUCH")
        edge(detector.observe(sample(event), true), event.eventTime)
    }

    fun hover(event: MotionEvent): Boolean {
        val k = eventKey(event)
        if (k == lastHoverKey) return lastHoverHandled
        lastHoverKey = k

        val hasPhysical = physical(event)
        if (event.pointerCount == 0) {
            lastHoverHandled = hasPhysical
            return hasPhysical
        }

        if (hasPhysical) clearInferred()

        val isEligible = eligible(event)
        edge(detector.observeHover(sample(event), isEligible), event.eventTime)

        if (isEligible && event.actionMasked == MotionEvent.ACTION_HOVER_EXIT) {
            handler.removeCallbacks(leaveRange)
            handler.postDelayed(leaveRange, 200L)
        } else if (isEligible && event.actionMasked == MotionEvent.ACTION_HOVER_ENTER) {
            handler.removeCallbacks(leaveRange)
        }

        lastHoverHandled = hasPhysical
        return hasPhysical
    }

    private fun physical(event: MotionEvent): Boolean {
        val target = StylusHardwareDetector.classifyMotionEvent(event)
        val down = event.actionMasked !in intArrayOf(
            MotionEvent.ACTION_BUTTON_RELEASE,
            MotionEvent.ACTION_CANCEL
        )
        tools.setPressed(
            BarrelToolController.Input.LOWER,
            down && target == StylusHardwareDetector.BarrelTarget.LOWER_BARREL
        )
        tools.setPressed(
            BarrelToolController.Input.UPPER,
            down && target == StylusHardwareDetector.BarrelTarget.UPPER_BARREL
        )
        return target != StylusHardwareDetector.BarrelTarget.NONE
    }

    private fun sample(event: MotionEvent) = ContactRestartDetector.Sample(
        event.actionMasked,
        event.eventTime,
        event.x,
        event.y,
        event.pressure,
        event.deviceId
    )

    private fun clearInferred() {
        tools.setPressed(BarrelToolController.Input.INFERRED_UPPER, false)
        detector.reset()
    }

    fun reset() {
        handler.removeCallbacks(leaveRange)
        detector.reset()
        tools.clear()
        lastTouchKey = null
        lastHoverKey = null
        lastHoverHandled = false
    }
}
