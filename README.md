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

## Macro Effects

- `thru`
- `filter`: 12/24dB multimode filter
- `eq`: parametric EQ and DJ-style kill EQ
- `mod`: 2-10 stage phaser, flanger, and 2-5 tap chorus
- `space`: spatializer, plate, spring, and dark reverb
- `texture`: comb filter, compressor, lo-fi, and ring-style motion
- `delay`: echo, dub delay, and freeze-style delay

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
- the selected `A` and `B` slots each run their own independent macro effect, amount, and four persisted macro values
- the engine crossfades between those scene outputs in real time
- all effect parameters are smoothed in SuperCollider to reduce zipper noise and clicks
- the live engine is self-contained in `lib/Engine_Fadddddddder.sc`; `lib/fx/` contains exploratory/reference FX classes
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
  lib/
    Engine_Fadddddddder.sc
    fx/
      Thru.sc
      Lowpass.sc
      Highpass.sc
      Dub.sc
      Microloop.sc
      Freeze.sc
      Drive.sc
      Chorus.sc
      Reverb.sc
      Ringmod.sc
      MacroFilter.sc
      MacroEQ.sc
      MacroMod.sc
      MacroSpace.sc
      MacroTexture.sc
      MacroDelay.sc
```

## Persistence

Scene settings are stored automatically in `data/fadddddddder/scene_bank.data`:

- crossfade
- selected `A` and `B` slot numbers
- all 16 `A` scene slots and all 16 `B` scene slots
- per-effect values inside each slot, so changing effects recalls that effect's own amount and macros

The bank saves whenever you edit slots, scene values, or the crossfader, and again on cleanup.

## Likely Next Steps

- add scene copy/capture actions
- add waveform or input/output metering
- tune the SC macros on actual norns hardware
- add params for input mode and gain staging
