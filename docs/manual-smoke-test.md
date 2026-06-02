# Manual smoke test

Use this on device after structural refactors.

## Test run metadata

| Field | Value |
|---|---|
| Date |  |
| Tester |  |
| Norns version |  |
| Script commit |  |
| Audio source used |  |
| Existing `scene_bank.data` present? | yes / no |
| Legacy flat bank used for migration check? | yes / no |

## Status key

- `PASS` — behavior matched expectations
- `FAIL` — behavior was incorrect or broken
- `N/A` — not applicable for this run
- `FOLLOW-UP` — partly worked, but needs another check

## Execution log

| Area | Step | Status | Notes |
|---|---|---|---|
| Boot | Start `fadddddddder` from the norns menu |  |  |
| Boot | Confirm the script loads without Lua errors |  |  |
| Boot | Confirm live audio passes through with default settings |  |  |
| Scene switching | On `perform`, turn `E2` and confirm side `A` slot changes |  |  |
| Scene switching | Hold `K1` and turn `E2` and confirm side `B` slot changes |  |  |
| Scene switching | Move `E3` and confirm the crossfader moves between `A` and `B` |  |  |
| Scene switching | Tap `K1` on `perform` and confirm the xfade snaps to center |  |  |
| Scene editing | Go to `scene A`, change effect, amount, and at least two params |  |  |
| Scene editing | Return to `perform` and confirm the updated effect label/value is shown |  |  |
| Scene editing | Go to `scene B` and repeat the edit check |  |  |
| Scene editing | Switch between filter-family presets and confirm shared params carry over smoothly |  |  |
| Persistence | Edit slots on both sides and move the crossfader away from default |  |  |
| Persistence | Exit the script or restart norns |  |  |
| Persistence | Relaunch and confirm slots, effects, params, and xfade are restored |  |  |
| Migration | Launch with an older saved bank and confirm it loads |  |  |
| Migration | Confirm both `A` and `B` lanes are populated after migration |  |  |
| Persistence regression | Save edits, relaunch, save different edits, and relaunch again |  |  |
| Persistence regression | Confirm repeated save/load cycles do not reset slots, params, or xfade unexpectedly |  |  |
| Corrupt data recovery | Replace `scene_bank.data` with malformed data and launch the script |  |  |
| Corrupt data recovery | Confirm the script logs a reset, boots successfully, and writes a fresh default bank |  |  |
| Engine params | Confirm changing scene params updates sound immediately |  |  |
| Engine params | Confirm `delay bpm` still affects the engine |  |  |
| Engine params | Confirm `delay sync` still affects the engine |  |  |
| Engine params | Confirm there are no clicks caused by broken scene sync or nil params |  |  |

## Persistence and migration setup notes

Use these setup cases when validating the persistence contract.

### Legacy flat-bank migration case

- prepare an older save that does not contain `bank.A` / `bank.B`
- launch once and confirm both lanes are populated from the shared legacy slots
- confirm the next save rewrites the file as the current dual-lane format

### Corrupt-data recovery case

- back up `data/fadddddddder/scene_bank.data`
- replace it with malformed data, or a table missing key nested structures
- launch the script
- confirm boot succeeds and a fresh default bank is written back to disk

### Repeated save/load case

- make one set of edits and relaunch
- make a second distinct set of edits and relaunch again
- confirm the second pass persists cleanly and the file does not regress to earlier values

## Failure capture

Record each failing or follow-up item in more detail.

| Step | Observed behavior | Expected behavior | Severity | Follow-up owner |
|---|---|---|---|---|
|  |  |  |  |  |

## Feature gate before new work

Use this checklist after the smoke pass and before starting a new feature.

| Gate | Result | Notes |
|---|---|---|
| Boot / scene-switch / persistence checks all passed or have accepted follow-ups |  |  |
| No new migration or corrupt-data regressions were introduced |  |  |
| The feature has a written module touch list before coding starts |  |  |
| The feature priority matches the backlog order in `README.md` |  |  |

### Current backlog order

1. **scene copy/capture actions**
   - expected modules: `lib/fadddddddder/pages.lua`, `lib/fadddddddder/scene_model.lua`, `lib/fadddddddder/store.lua`
2. **gain staging and input params**
   - expected modules: `lib/fadddddddder/params.lua`, `lib/fadddddddder/engine_adapter.lua`, `lib/Engine_Fadddddddder.sc`
3. **metering and visual additions**
   - expected modules: `lib/fadddddddder/pages.lua` and any new UI helper module added for drawing/state flow

## End-of-run summary

- Overall result: PASS / FAIL / FOLLOW-UP
- Safe to continue refactoring? yes / no
- Safe to start the next backlog feature? yes / no
- Safe to cut a release candidate? yes / no
- Highest-risk area from this run:
- Recommended next action:
