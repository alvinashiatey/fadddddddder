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
| Engine params | Confirm changing scene params updates sound immediately |  |  |
| Engine params | Confirm `delay bpm` still affects the engine |  |  |
| Engine params | Confirm `delay sync` still affects the engine |  |  |
| Engine params | Confirm there are no clicks caused by broken scene sync or nil params |  |  |

## Failure capture

Record each failing or follow-up item in more detail.

| Step | Observed behavior | Expected behavior | Severity | Follow-up owner |
|---|---|---|---|---|
|  |  |  |  |  |

## End-of-run summary

- Overall result: PASS / FAIL / FOLLOW-UP
- Safe to continue refactoring? yes / no
- Safe to cut a release candidate? yes / no
- Highest-risk area from this run:
- Recommended next action:
