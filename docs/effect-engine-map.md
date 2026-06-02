# Effect and engine coupling map

This document records the exact places that must stay in sync when you add, remove, rename, or remap an effect.

## Source-of-truth chain

There are four linked layers:

1. `lib/fadddddddder/effects.lua`
   - each preset declares an `id`
   - each preset points at an engine family via `engine = "..."`
   - `Effects.engine_map[effect_id]` is derived from that field
2. `lib/fadddddddder/effects.lua`
   - `Effects.engine_effect_index` maps each engine family name to the integer sent to SuperCollider
3. `lib/fadddddddder/engine_adapter.lua`
   - `effect_engine_index_for()` converts the selected preset id into the engine-family integer used by the SC engine
4. `lib/Engine_Fadddddddder.sc`
   - the `sceneAEffect` / `sceneBEffect` integer branch ordering must match `Effects.engine_effect_index`
   - the top-of-file effect-index comment must describe the same active contract
   - each branch interprets `param1..param4` differently

If any one of those layers changes without the others, the wrong DSP branch will load or the right branch will receive the wrong parameter meaning.

## Active engine family map

These indices are the ones currently reachable from Lua:

| Index | Engine family | Lua key | Param meaning in presets |
| --- | --- | --- | --- |
| 0 | thru | `thru` | tone, gain |
| 1 | multimode filter | `filter` | cutoff, res, mode, slope |
| 2 | parametric EQ/drive | `eq` | freq, gain, q, style |
| 3 | modulation | `mod` | rate, depth, type, stages |
| 4 | space | `space` | size, damp, type, width |
| 5 | texture | `texture` | color, damage, type, motion |
| 6 | delay | `delay` | time, feedback, freeze, tone |
| 7 | resonator | `resonator` | tune, decay, body, tone |
| 8 | folded filter | `fold_filter` | cutoff, res, mode, fold |
| 9 | formant | `formant` | vowel, spread, focus, tone |
| 10 | tremolo | `tremolo` | rate, depth, shape, bias |
| 11 | crusher | `crusher` | rate, bits, mix, tone |
| 12 | frequency shifter | `freq_shift` | shift, spread, mix, tone |
| 13 | granular | `granular` | density, size, scatter, tone |
| 14 | plate | `plate` | size, decay, pre, tone |
| 15 | early reflections | `early` | size, spread, cluster, tone |
| 16 | haas | `haas` | time, spread, tilt, tone |
| 17 | tape saturation | `tape_sat` | drive, bias, mix, tone |
| 18 | transient | `transient` | attack, sustain, mix, tone |
| 19 | multiband clip | `multiclip` | drive, tilt, mix, tone |

## Preset groups by engine family

These are the preset ids currently routed into each family:

- `thru`: `thru`
- `filter`: `low_pass`, `band_pass`, `high_pass`, `muffled_bloom`, `radio_tunnel`, `airlift`, `peak_sweep`, `dark_shelf`, `thin_ice`, `sub_cut`, `vowel_sweep`, `needle_focus`
- `eq`: `pressure_drive`
- `mod`: `phaser`, `flanger`, `hollow_phase`, `jet_wash`, `stereo_bloom`, `comb_drift`
- `space`: `air_widen`, `washed_hall`, `spring_tunnel`
- `texture`: `compressor`, `limiter`, `glue_pump`, `brick_squeeze`, `crushed_bloom`, `metal_gate`, `broken_tape`, `metal_room`, `glass_bite`
- `delay`: `burn_echo`, `freeze_haze`, `dub_bloom`
- `resonator`: `karplus_bloom`, `metal_string`
- `fold_filter`: `folded_resonance`, `acid_fold`
- `formant`: `vowel_formant`, `choir_formant`
- `tremolo`: `tremolo`, `gate_chop`
- `crusher`: `bit_crush`, `data_melt`
- `freq_shift`: `freq_shift`, `robot_drift`
- `granular`: `grain_freeze`, `grain_scatter`
- `plate`: `plate_bright`, `plate_bloom`
- `early`: `early_bounce`, `room_slap`
- `haas`: `haas_widen`, `mono_lift`
- `tape_sat`: `tape_sat`, `warm_glue`
- `transient`: `transient_tame`, `sustain_bloom`
- `multiclip`: `tri_clip`, `band_burn`

## Important maintenance rules

### Adding a new preset to an existing engine family

Only update `lib/fadddddddder/effects.lua` if:

- the preset can reuse an existing `engine = "..."` family
- the family already interprets `param1..param4` the way you need

No SC index change is required in that case.

### Adding a new engine family

You must update all of these together:

1. add the new preset definition(s) in `lib/fadddddddder/effects.lua`
2. add the new family name to `Effects.engine_effect_index`
3. add the SC DSP branch in `lib/Engine_Fadddddddder.sc`
4. insert the branch at the exact matching SelectX index
5. confirm the branch uses the same `param1..param4` meaning documented by the preset labels
6. smoke-test scene save/load plus live switching on hardware

### Renaming an engine family key

If you rename keys such as `fold_filter` or `freq_shift`, you must update:

- preset `engine =` fields in `effects.lua`
- `Effects.engine_effect_index`
- any Lua code depending on that family key
- this document

### Renaming a preset id

If you rename a preset id, update:

- the preset `id` in `effects.lua`
- any legacy migration mapping if that preset is used as a fallback
- any saved-bank migration logic that depends on that id

## Current caveat in the SC file

`lib/Engine_Fadddddddder.sc` still documents extra branches at indices `20..25` (`pitchShift`, `reverse`, `slapback`, `convReverb`, `spectralFreeze`, `autoWah`).

Those branches are not currently exposed by `Effects.engine_effect_index`, so Lua can only reach indices `0..19`.

Treat `0..19` as the active public contract unless you intentionally wire those additional SC branches back into Lua and update this document at the same time.
