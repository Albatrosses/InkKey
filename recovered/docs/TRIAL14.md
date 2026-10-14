# Trial 14

Responds to the Trial 13 regression report. Restores the earlier default contact movement rule; the unverified hover-trajectory expansion is now opt-in only in developer tests, not used by the app. Pressure and hover timing thresholds are unchanged. Raw motion patterns in the uploaded Trial 13 trace classify identically under the old/new movement rules, so that trace cannot establish the degradation's cause.

A physical lower button no longer deletes an independently inferred upper hold; palm/finger input no longer clears physical pen button state. Real upper bits retain precedence. This repairs a routing inconsistency with the already tested independent-button controller.

Symbol controls have their own elevated hit target, including padding between buttons. IME selection survives hiding; the editor holds a symbol snapshot and saves by identity rather than depending on transient selection. Keyboard composition is disposed when the service lifecycle is destroyed, matching the verified clean APK, rather than on every view detach. Native touch tests exercise button presses over an underlying pointerInteropFilter canvas.

Motion diagnostics now retain stylus/eraser events even when Android does not expose the expected device identity, and include pointer count and device ID. This improves diagnosis of missing keyboard delivery and eligibility checks. The uploaded log had main motion routes and an IME_SHOWN marker but no keyboard motion/key events.

Preserves Trial 12 shape/droplet improvements and Trial 13 main/IME key ownership and shared mapping refresh. Physical verification is still required; no button accuracy percentage is claimed.
