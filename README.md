# fadddddddder

An octo-inspired norns scene fader for live input.

`fadddddddder` stores two scene states (`A` and `B`) and morphs between them with a single encoder while a dedicated SuperCollider engine handles the live audio path.

## Status

First pass. The core workflow is there:

- assign an effect macro to scene `A`
- assign an effect macro to scene `B`
- set each scene amount plus effect-specific macro parameters
- sweep the crossfader between them on the performance page
- store and recall scene settings with norns psets

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

- `E3`: crossfade from `A` to `B`
- `K1`: snap to center

### Scene pages

- `E2`: select field
- `E3`: edit selected field
- `K1`: jump the crossfader to that scene for quick auditioning

## Pages

- `perform`: main `[A] -|- [B]` crossfader view
- `scene A`: choose effect, amount, and macro parameters for scene `A`
- `scene B`: choose effect, amount, and macro parameters for scene `B`

## Audio Model

This is a live-input SuperCollider processor with a scene crossfader.

- incoming audio is processed by a custom engine in `lib/Engine_Fadddddddder.sc`
- scene `A` and scene `B` each run their own macro effect, amount, and four persisted macro values
- the engine crossfades between those scene outputs in real time
- all effect parameters are smoothed in SuperCollider to reduce zipper noise and clicks
- all effects are generated in the engine using classes in `lib/fx/`
- crossfade, scene effects, scene amounts, and macro values are backed by norns params for pset persistence

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

Scene settings are stored as norns params:

- crossfade
- scene A effect, amount, macro 1, macro 2, macro 3, macro 4
- scene B effect, amount, macro 1, macro 2, macro 3, macro 4

Use the normal norns pset workflow to save and recall these values.

## Likely Next Steps

- add scene copy/capture actions
- add waveform or input/output metering
- tune the SC macros on actual norns hardware
- add params for input mode and gain staging
