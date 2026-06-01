# fadddddddder architecture

## Overview

`fadddddddder` is organized as a small main script plus focused modules.

- `fadddddddder.lua` — lifecycle orchestration, params, engine boot, top-level wiring
- `lib/fadddddddder/effects.lua` — effect catalog and derived lookup tables
- `lib/fadddddddder/scene_model.lua` — scene slot creation, cloning, sanitization, cursor/value helpers
- `lib/fadddddddder/store.lua` — bank migration, load/save, debounced persistence flag
- `lib/fadddddddder/engine_adapter.lua` — Lua → SuperCollider engine sync
- `lib/fadddddddder/pages.lua` — page navigation, input handling, and screen drawing
- `lib/Engine_Fadddddddder.sc` — live DSP engine

## Runtime data flow

1. `init()` wires params, loads the stored bank, and boots the engine.
2. `pages.lua` mutates `state` in response to encoders/keys.
3. Scene edits call `request_save()` and `apply_bundle()`.
4. `engine_adapter.lua` pushes scene A/B values and xfade into the engine.
5. `store.lua` flushes pending saves during the redraw metro.

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
- change controls or screen behavior in `pages.lua`
