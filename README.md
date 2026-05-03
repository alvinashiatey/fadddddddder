# fadddddddder

An octo-inspired norns scene fader for live input.

`fadddddddder` stores two scene states (`A` and `B`) and morphs between them with a single encoder while a dedicated SuperCollider engine handles the live audio path.

## Status

First pass. The core workflow is there:

- assign an effect macro to scene `A`
- assign an effect macro to scene `B`
- set each scene amount
- sweep the crossfader between them on the performance page

This version is intentionally small, but the live DSP now runs in a custom SuperCollider engine instead of `softcut` transport tricks.

## Effects In v0.1

- `thru`
- `lowpass`
- `highpass`
- `dub echo`
- `microloop`
- `freeze`

## Controls

### Global

- `E1`: change page
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
- `scene A`: choose effect and amount for scene `A`
- `scene B`: choose effect and amount for scene `B`

## Audio Model

This is a live-input SuperCollider processor with a scene crossfader.

- incoming audio is processed by a custom engine in `lib/Engine_Fadddddddder.sc`
- scene `A` and scene `B` each run their own effect macro and amount
- the engine crossfades between those scene outputs in real time
- all effect parameters are smoothed in SuperCollider to reduce zipper noise and clicks
- `thru`, `lowpass`, `highpass`, `dub echo`, `microloop`, and `freeze` are all generated in the engine

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
```

## Likely Next Steps

- add PARAMS for persistence and pset support
- add per-effect sub-parameters
- add scene copy/capture actions
- add waveform or input/output metering
- tune the SC macros on actual norns hardware
- add params for input mode and gain staging
