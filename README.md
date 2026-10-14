# InkKey

InkKey is an Android handwriting workspace and handwriting keyboard for working through Physics, Chemistry, Biology, and mathematics. Write with a pen tablet, organize pages, insert editable scientific symbols, recognize drawn shapes, and share your work as images or PDFs with ChatGPT or another app.

## Features

- Pressure-sensitive writing, configurable pen and eraser tools, undo/redo, page management, and paper styles.
- Standalone canvas and Android input-method keyboard, with tablet settings and shared express-key mappings.
- Editable, movable, resizable, and rotatable text and scientific symbols.
- Shape recognition for lines, arrows, circles, ellipses, triangles, and rectangles.
- A narrow, borderless droplet handle for opening the floating toolbar.
- Temporary barrel-button tools that return to the selected tool on release.
- Image and PDF export and Android sharing.

## Install

Open the latest successful **InkKey** run in [GitHub Actions](https://github.com/Albatrosses/InkKey/actions), download **InkKey-3.0**, extract the ZIP, and install `InkKey-3.0.apk`. Enable and select **InkKey** in Android's keyboard settings to use the keyboard interface.

The maintained app uses package `com.inkkey.android`. It installs separately from earlier experimental apps; existing notes in those apps are not automatically migrated. Export any notes you want to keep before removing older installations.

## Source and builds

This repository contains the complete maintained native Kotlin/Jetpack Compose source. The app lives in `app/src/main`, tests in `app/src/test`, dependency versions in `gradle/libs.versions.toml`, and the build workflow in `.github/workflows/build.yml`. No source ZIP extraction or patch scripts are needed.

Use JDK 21, Gradle 9.3.1, and the Android SDK required by `app/build.gradle.kts`. Copy `.env.example` to `.env` and provide a signing keystore as described in [maintenance notes](docs/MAINTENANCE.md), then run:

```sh
gradle testDebugUnitTest lintRelease assembleRelease --no-daemon
```

CI runs the full JVM/Robolectric test suite, Android lint, and builds the signed APK. Physical tablet behavior still requires hardware testing; passing automated tests does not establish perfect upper-button accuracy.

## Upper barrel button

On the tested XP-Pen Deco Mini7 V2 / Android setup, the upper button does not report a dedicated Android button bit in the captured events. InkKey infers temporary upper-button state from specific hover and pressure-bearing contact restarts. Hover press matching includes the recorded 46-ms / 132.7-pixel and 52-ms restarts; ordinary tip lifts retain separate safeguards. Detection may still miss or misclassify events on some hardware. Other tablets with explicit button bits use those directly.

## Development and Codex contributions

Albertrosses directed the project, tested the tablet repeatedly, and provided APKs, screenshots, and event reports that exposed the upper-button restart signature.

Codex assisted with recovering maintainable native source and reconstructing input behavior from the surviving APK after the earlier local source was lost. Subsequent work includes shared tablet/express-key routing, independent temporary barrel-tool state, keyboard input ownership, symbol selection and editing fixes, shape-fitting improvements, the droplet toolbar handle, log-based upper-button regression tests, and automated build verification. This is a maintained reconstruction and development effort, not a claim that it reproduces every detail of the deleted source.

Old source snapshots, inline patch scripts, and duplicate workflow helper files have been removed from the current tree. Git history and the earlier development branch remain available for recovery.
