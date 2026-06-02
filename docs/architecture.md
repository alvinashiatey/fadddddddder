# fadddddddder architecture

## Overview

`fadddddddder` is organized as a tiny entry script plus focused modules.

- `fadddddddder.lua` — lifecycle handoff only
- `lib/fadddddddder/app.lua` — application wiring, boot flow, and top-level orchestration
- `lib/fadddddddder/params.lua` — norns param registration and callbacks
- `lib/fadddddddder/effects.lua` — effect catalog and derived lookup tables
- `lib/fadddddddder/scene_model.lua` — scene slot creation, cloning, sanitization, cursor/value helpers
- `lib/fadddddddder/store.lua` — bank migration, load/save, debounced persistence flag
- `lib/fadddddddder/engine_adapter.lua` — Lua → SuperCollider engine sync
- `lib/fadddddddder/pages.lua` — page navigation, input handling, and screen drawing
- `lib/Engine_Fadddddddder.sc` — live DSP engine

## Runtime data flow

1. `fadddddddder.lua` forwards lifecycle hooks into `app.lua`.
2. `app.lua` registers params, loads the stored bank, and boots the engine.
3. `pages.lua` mutates scene state in response to encoders/keys.
4. Scene edits call `request_save()` and `apply_bundle()`.
5. `engine_adapter.lua` pushes scene A/B values and xfade into the engine.
6. `store.lua` flushes pending saves during the redraw metro.

## Persisted state

The saved bank contains:

- `slots.A`, `slots.B`
- `xfade`
- `bank.A[1..16]`
- `bank.B[1..16]`

Each slot stores:

- `effect`
- `values[effect_id] = { amount, param1, param2, param3, param4 }`

## Invariants

- every bank always has `A` and `B` lanes with 16 slots each
- every slot always resolves to a valid effect id
- every effect value is clamped to `0..1`
- old flat v1 banks are migrated into dual-lane banks on load
- UI edits do not write to disk immediately; they set a pending-save flag

## Refactor intent

The goal of this layout is to keep future changes local:

- add or tune effects in `effects.lua`
- change slot/bank rules in `scene_model.lua` or `store.lua`
- change engine parameter wiring in `engine_adapter.lua`
- change boot wiring in `app.lua` or `params.lua`
- change controls or screen behavior in `pages.lua`
