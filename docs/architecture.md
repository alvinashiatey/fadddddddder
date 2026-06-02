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

## Maintainer docs

- `docs/architecture.md` — module boundaries, state ownership, and persistence contract
- `docs/effect-engine-map.md` — effect-family routing and Lua-to-SC coupling points
- `docs/manual-smoke-test.md` — post-refactor verification checklist

## Runtime data flow

1. `fadddddddder.lua` forwards lifecycle hooks into `app.lua`.
2. `app.lua` creates the owned state tables, registers params, loads the stored bank, and boots the engine.
3. `app.lua` exposes a small action layer for page navigation, scene edits, xfade changes, and page view models.
4. `pages.lua` uses that compact action API for encoders, keys, and redraws instead of reaching into effect/store internals directly.
5. Scene edits call `request_save()` and `apply_bundle()` through the action layer.
6. `engine_adapter.lua` pushes scene A/B values and xfade into the engine.
7. `store.lua` flushes pending saves during the redraw metro.

## State ownership

`lib/fadddddddder/state.lua` defines four scoped tables:

- `persisted` — `xfade`, selected slots, and both scene banks
- `ui` — current page and cursor position
- `interaction` — transient key-modifier state such as `K1` holds
- `runtime` — engine readiness and metro handles

Only `persisted` is loaded from disk or saved back to disk. Runtime-only values never enter the persistence layer.

## Persistence contract

The bank file lives at `data/fadddddddder/scene_bank.data` and is always rewritten as a version 2 payload.

Saved payload shape:

```lua
{
  version = 2,
  slots = { A = 1, B = 1 },
  xfade = 0.0,
  bank = {
    A = {
      [1] = {
        effect = "dub_bloom",
        values = {
          dub_bloom = { amount = 0.6, param1 = 0.48, param2 = 0.68, param3 = 0.25, param4 = 0.42 },
          -- one entry for every effect id in effects.lua
        },
      },
      -- slots 2..16
    },
    B = {
      -- slots 1..16
    },
  },
}
```

Required persisted fields:

- `slots.A`, `slots.B`
- `xfade`
- `bank.A[1..16]`
- `bank.B[1..16]`

Each slot stores:

- `effect`
- `values[effect_id] = { amount, param1, param2, param3, param4 }`

## Migration and failure behavior

Supported migration path:

- legacy flat-bank saves are treated as version 1-style data
- if `bank.A` / `bank.B` are missing, `store.lua` duplicates the shared flat slots into both lanes
- every recovered slot is sanitized through `scene_model.lua` before use

Failure behavior:

- missing or non-table payloads fall back to the in-memory defaults from `state.lua`, then get rewritten as version 2 on save
- malformed slot/effect/value data is clamped or replaced during sanitization
- if sanitization raises an error, `store.lua` deletes `scene_bank.data`, resets persisted state to defaults, and immediately writes a fresh version 2 bank
- save requests are debounced in memory; edits mark `save_pending`, and the redraw metro flushes the write

## Invariants

- every bank always has `A` and `B` lanes with 16 slots each
- every slot always resolves to a valid effect id
- every effect value is clamped to `0..1`
- old flat v1 banks are migrated into dual-lane banks on load
- UI edits do not write to disk immediately; they set a pending-save flag

## Refactor intent

The goal of this layout is to keep future changes local:

- add or tune effects in `effects.lua`
- keep effect-family routing aligned across `effects.lua`, `engine_adapter.lua`, and `Engine_Fadddddddder.sc` using `docs/effect-engine-map.md`
- change slot/bank rules in `scene_model.lua` or `store.lua`
- change engine parameter wiring in `engine_adapter.lua`
- change boot wiring in `app.lua` or `params.lua`
- change controls or screen behavior in `pages.lua` while keeping the action contract in `app.lua` stable
