# Recovered input baseline and upper-button trial 6

The baseline project is the editable v1.8 workflow source, with verified PC input behavior reconstructed from the final clean APK (SHA256 1c32cfed92c8a05b888bfd477f3fb7fcf407b8ebfc2a2cb30eadca68fc985335).

Restored input behavior: lower eraser-tool classification; secondary mouse button classification; XP-Pen B/E/Alt/Space/Ctrl+S/Ctrl+Z express mappings ahead of barrel classification; Space/K4 activity and preview handling; temporary barrel tools in both main workspace and IME; configurable swapped actions. The old zero-pressure permanent-toggle guess is removed from the IME. Selected shape snapping, the 0.1 zoom minimum/increments and the IME undo history cap of 30 are also restored.

The upper-button contact/hover detector is an inference because this hardware's Android MotionEvents report buttonState=0. Trial 6 increases the hover press movement allowance from 18 to 24 pixels to include the recorded 18.76-pixel / 44-ms pair. Contact pressure thresholds are unchanged. Upper release returns to the selected toolbar tool; inferred state survives a tip lift while hovering, but resets after 200 ms outside hover range, cancel, non-stylus contact, or app pause. Candidate popups are disabled.

This is a reconstruction of verified behavior, not a claim of an exact source or UI match to the deleted PC project. Full application/UI parity has not been established. Unit tests and compilation establish code behavior; physical input reliability still requires testing on the user's tablet.

The debug application has a separate package (`com.aistudio.inkkey.recoveredtrial6`) to allow testing without replacing or uninstalling the existing app or its notes. The build's debug signing key is generated per run; this package is for a fresh test install. For a production update to the original package, its original signing key is required.

Source is now stored as ordinary editable Kotlin and Gradle files under recovered/. The workflow builds those files directly; it no longer unpacks old ZIP source or runs inline patch scripts. The private recovery archive separately preserves the decompiled references and method bytecode.

## Build

Use JDK 21, Gradle 9.3.1 and an Android SDK supporting the project's compile SDK. CI runs:

    gradle testDebugUnitTest --tests com.example.ContactRestartDetectorTest --tests com.example.BarrelToolControllerTest --tests com.example.RecoveredInputTest assembleDebug --no-daemon

The Actions artifact `InkKey-Recovered-Trial-6` contains the APK. The `InkKey-Input-Test-Reports` artifact contains regression results.
