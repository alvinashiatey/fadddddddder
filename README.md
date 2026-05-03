# fadddddddder

An octo-inspired norns scene fader for live input.

`fadddddddder` records incoming audio into a rolling `softcut` buffer, stores two scene states (`A` and `B`), and lets you morph between them with a single encoder.

## Status

First pass. The core workflow is there:

- assign an effect macro to scene `A`
- assign an effect macro to scene `B`
- set each scene amount
- sweep the crossfader between them on the performance page

This version is intentionally small and uses stock `softcut` features only.

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
- `K2` / `K3`: previous / next page on scene pages

### Perform page

- `E3`: crossfade from `A` to `B`
- `K2`: snap to `A`
- `K1`: snap to center
- `K3`: snap to `B`

### Scene pages

- `E2`: select field
- `E3`: edit selected field
- `K1`: reframe the loop playheads to the loop start

## Pages

- `perform`: main `[A] -|- [B]` crossfader view
- `scene A`: choose effect and amount for scene `A`
- `scene B`: choose effect and amount for scene `B`

## Audio Model

This is a live-input `softcut` processor, not a zero-latency insert effect rack.

- incoming audio is routed into `softcut`
- two mono `softcut` voices handle left/right
- scenes generate parameter bundles
- the crossfader interpolates those bundles in real time

That means the script behaves more like a scene-morphing live buffer processor than a simple dry/wet pedal.

## Install

After this repo is on GitHub:

```text
;install https://github.com/alvinashiatey/fadddddddder
```

Repo:

```text
https://github.com/alvinashiatey/fadddddddder.git
```

## File Layout

```text
fadddddddder/
  fadddddddder.lua
  README.md
```

## Likely Next Steps

- add PARAMS for persistence and pset support
- add per-effect sub-parameters
- add scene copy/capture actions
- add waveform or input/output metering
- test and tune loop timing on actual norns hardware
