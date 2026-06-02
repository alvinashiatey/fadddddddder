# fadddddddder

An octo-inspired norns scene fader for live input.

`fadddddddder` stores two independent 16-slot scene banks, assigns one `A` slot and one `B` slot, and morphs between them while a dedicated SuperCollider engine handles the live audio path.

## Status

First pass. The core workflow is there:

- assign independent scene slots to fader side `A` and `B`
- set each slot's macro effect, amount, and effect-specific macro parameters
- sweep the crossfader between them on the performance page
- store and recall both 16-slot scene banks automatically

This version is intentionally small, but the live DSP now runs in a custom SuperCollider engine instead of `softcut` transport tricks.
The visible effects are curated presets built from a CPU-safe modular backend.

## Preset Effects

- `thru`
- `air widen`: highpass into stereo widening
- `washed hall`: highpass into dark hall reverb
- `spring tunnel`: bandpass into spring space
- `burn echo`: drive into delay
- `broken tape`: lo-fi wear into chorus wobble
- `metal room`: ring modulation into dark room
- `comb drift`: comb resonance into chorus drift
- `freeze haze`: highpass into freeze-style hold
- `dub bloom`: bandpass into dub repeats
- `glass bite`: highpass into ring modulation
- `pressure drive`: focused EQ into drive

## Controls

### Global

- `E1`: change page, clamped at `perform` and `scene B`
- `K2` / `K3`: previous / next page

### Perform page

- `E2`: select `A` scene slot
- `E3`: crossfade from `A` to `B`
- hold `K1` + `E2`: select `B` scene slot
- tap `K1`: snap to center

### Scene pages

- `E2`: select field
- `E3`: edit selected field
- `K1`: jump the crossfader to that scene for quick auditioning

## Pages

- `perform`: select scene slots and crossfade between `A` and `B`
- `scene A`: edit the scene slot assigned to `A`
- `scene B`: edit the scene slot assigned to `B`

## Audio Model

This is a live-input SuperCollider processor with a scene crossfader.

- incoming audio is processed by a custom engine in `lib/Engine_Fadddddddder.sc`
- the selected `A` and `B` slots each run their own independent preset effect, amount, and four persisted macro values
- the engine crossfades between those scene outputs in real time
- all effect parameters are smoothed in SuperCollider to reduce zipper noise and clicks
- the live engine is self-contained in `lib/Engine_Fadddddddder.sc`
- each preset is internally limited to two serial DSP stages for CPU safety
- both 16-slot scene banks are saved to `data/fadddddddder/scene_bank.data`

That means simple filter moves no longer depend on `softcut` head jumps, which was the main source of the clicking and accidental short-loop behavior.

## Install

After this repo is on GitHub:

```text
;install https://github.com/alvinashiatey/fadddddddder
```

Repo:

```text
https://github.com/alvinashiatey/fadddddddder.git
```

After installing or updating, restart norns so it recompiles the custom engine.

## File Layout

```text
fadddddddder/
  fadddddddder.lua
  README.md
  .gitignore
  docs/
    architecture.md
    manual-smoke-test.md
  lib/
    Engine_Fadddddddder.sc
    fadddddddder/
      app.lua
      effects.lua
      engine_adapter.lua
      pages.lua
      params.lua
      scene_model.lua
      store.lua
```

## Architecture

The script is now organized as a tiny top-level entrypoint plus focused modules:

- `fadddddddder.lua`: lifecycle handoff only
- `lib/fadddddddder/app.lua`: application wiring, boot flow, and module orchestration
- `lib/fadddddddder/params.lua`: param registration and callbacks
- `lib/fadddddddder/effects.lua`: effect catalog and lookup tables
- `lib/fadddddddder/scene_model.lua`: slot defaults, cloning, sanitization, cursor/value helpers
- `lib/fadddddddder/store.lua`: bank migration, load/save, pending-save debounce
- `lib/fadddddddder/engine_adapter.lua`: Lua-to-engine sync
- `lib/fadddddddder/pages.lua`: page navigation, input handling, and redraw logic

For maintainer-focused details, see:

- `docs/architecture.md`
- `docs/manual-smoke-test.md`

## Persistence

Scene settings are stored automatically in `data/fadddddddder/scene_bank.data`:

- crossfade
- selected `A` and `B` slot numbers
- all 16 `A` scene slots and all 16 `B` scene slots
- per-effect values inside each slot, so changing effects recalls that effect's own amount and macros

The bank saves whenever you edit slots, scene values, or the crossfader, and again on cleanup.

## Likely Next Steps

- tune preset gain staging on actual norns hardware
- add scene copy/capture actions
- add waveform or input/output metering
- add params for input mode and gain staging
