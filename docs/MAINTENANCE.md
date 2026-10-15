# Maintaining InkKey

## Preserve these files

Keep `app/` (source, resources, manifest, tests, schema/build configuration), root Gradle configuration, `gradle/`, `.env.example`, `.gitignore`, this documentation, and the Actions workflow. Commit future source changes before relying on a built APK. The source in `main` is authoritative; old Actions APKs are not substitutes for it.

Git history preserves earlier snapshots. Do not rewrite history or delete the maintained source when cleaning build outputs. Ignore generated build folders, private environment files, and signing keys.

## Signing and future updates

Android updates require the same application ID and signing key. CI accepts a private base64 keystore in `INKKEY_SIGNING_KEY_BASE64`, with optional `INKKEY_STORE_PASSWORD`, `INKKEY_KEY_PASSWORD`, and `INKKEY_KEY_ALIAS` secrets. Never commit the keystore or passwords to this public repository.

CI encrypts a recovery copy of its signing keystore using the public certificate in `ci/signing-backup.crt`. The `InkKey-Encrypted-Signing-Backup` Actions artifact and encrypted log payload contain no plaintext private key. The matching recovery private key and initial signing keystore are preserved in the owner's separate **InkKey-signing-backup.zip**. Keep that ZIP private and keep a personal offline copy; never commit its contents. To restore a later encrypted backup:

```sh
openssl cms -decrypt -binary -inform DER -in InkKey-signing-backup.enc -recip signing-backup.crt -inkey backup-private.pem -out debug.keystore
```

Without signing secrets, CI generates a development key and caches it under `inkkey-signing-v1`. Cache retention is not a durable signing-key backup: if it is evicted, a newly generated key cannot update a previous installation. Before distributing long-lived updates, retain the original key securely and configure the signing secrets. The default key uses alias `androiddebugkey` and password `android`. Local builds accept `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`, and `KEY_ALIAS`; the default file is `debug.keystore` at the project root.

## Input architecture

`BarrelInputRouter` deduplicates root/canvas event deliveries and feeds `ContactRestartDetector` and `BarrelToolController`. Explicit Android button bits take precedence. Lower and upper held states are independent; lower temporarily overrides upper. Finger contacts must not release pen-button state. Hover departure clears inferred state after 200 ms, and app/IME lifecycle transitions reset temporary tools.

For the recorded device (vendor 0x28BD, product 0x0948), hover press candidates require 42–52 ms, zero pressure, the same device, and movement at most 160 pixels. Release candidates require 30–40 ms and at most 160 pixels. Contact inference retains pressure above 0.02 on both endpoints, a gap of 1–100 ms, a bounded movement allowance, and pressure ratios >=1.7 for press or <=0.65 for release. Do not lower the contact threshold without labeled hardware recordings distinguishing button presses from ordinary lifts.

`XpPenExpressKeyResolver` is shared by the main app and keyboard. Verified physical mappings: K1 B, K2 E, K3 Left Alt, K4 Space, K5 Ctrl+S, K6 Ctrl+Z, K7 numpad plus/scan 78, K8 numpad minus/scan 74. Learned key/scan pairs require both when both were learned; ambiguous matches are rejected.

Symbol controls have a pointer guard above the canvas. The keyboard editor retains a symbol identity so temporary selection changes cannot dismiss or redirect a save. Its Compose view follows the service lifecycle rather than disposing on a temporary window detach.

## Regression coverage

Keep the captured-event tests and negative controls alongside shape, input ownership, express-key, symbol editing/touch, persistence, undo, export, and keyboard tests. CI runs all tests rather than a selected subset. Run Android lint and rebuild after source edits. Hardware checks should include press while hovering, press during writing, release, lower/upper overlap, keyboard-to-main transitions, and symbol Edit/Save taps.

The upper-button trial log UI and runtime recorder were removed from the maintained app. If further investigation is needed, add opt-in diagnostics on a development branch and keep them out of normal settings.
