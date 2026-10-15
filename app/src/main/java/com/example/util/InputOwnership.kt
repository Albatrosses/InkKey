package com.example.util

object InputOwnership {
    /** A hidden input method must let hardware events reach the foreground application. */
    fun imeOwnsHardwareInput(inputViewShown: Boolean): Boolean = inputViewShown

    fun routeImeHardwareKey(inputViewShown: Boolean, handle: () -> Boolean): Boolean =
        inputViewShown && handle()
}
