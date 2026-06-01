# Manual smoke test

Run this checklist on device after structural refactors.

## Boot

- start `fadddddddder`
- confirm script loads without Lua errors
- confirm audio passes through with default settings

## Scene selection

- on `perform`, turn `E2` and confirm side `A` slot changes
- hold `K1` and turn `E2` and confirm side `B` slot changes
- move `E3` and confirm the crossfader moves between `A` and `B`
- tap `K1` on `perform` and confirm the xfade snaps to center

## Scene editing

- go to `scene A`
- change effect, amount, and at least two params
- confirm returning to `perform` shows the updated effect label/value
- go to `scene B` and repeat
- switch between filter-family presets and confirm shared params carry over smoothly

## Persistence

- edit slots on both sides
- move the crossfader away from default
- exit the script or restart norns
- relaunch and confirm slots, effects, params, and xfade are restored

## Migration safety

- if you have an older saved bank, launch once and confirm it loads
- confirm both `A` and `B` lanes are populated after migration

## Engine behavior

- confirm changing scene params updates sound immediately
- confirm delay bpm and delay sync params still affect the engine
- confirm there are no clicks caused by broken scene sync or nil params
