# Trial 13

Preserves Trial 12 shape and droplet work. A hidden IME now passes hardware keys through instead of consuming them for its own document. Main input reloads shared mappings even when keyboard changes do not pause the activity; descendant focus is allowed so text dialogs work.

Symbol bodies drag before resize hit areas, displayed selection frames follow rotation, the topmost overlapping symbol is selected, and the action bar stays within view at zoom. Both canvases have an Edit action for symbol text, preserving identity/position/style with undo. IME drag continues synchronously rather than depending on the next tool recomposition.

The trial_7 uploaded trace contains an unclassified 12 ms touch restart with approximately 108 px motion, 0.06116 -> 0.14674 pressure, and intermediate hover positions supporting that movement. The detector uses that trajectory to adjust its existing movement allowance, capped at 160 px. The pressure and hover-timing thresholds remain unchanged. This is an inferred button candidate, not proof of a physical press. Ordinary low-pressure tip releases and uncorroborated large jumps remain rejected.

New captures identify the actual version, MAIN/IME sessions, and tablet key routes/visibility. The supplied trial_6/trial_7 files contained only main motion routes and no key records; they cannot establish what the keyboard received. For physical testing, open the Trial 13 app and select its Trial 13 keyboard in Android, then capture a main -> keyboard -> main sequence.
