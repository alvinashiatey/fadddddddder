-- fadddddddder: octo-inspired scene crossfader for live input
-- v0.7.0 @alvinashiatey

engine.name = "Fadddddddder"

local tabutil = require("tabutil")

local pages = { "perform", "scene_a", "scene_b" }
local page_labels = { perform = "perform", scene_a = "scene A", scene_b = "scene B" }

local effect_definitions = {
	{
		id = "thru",
		engine = "thru",
		label = "thru",
		params = { "tone", "gain" },
		param_count = 2,
		defaults = { amount = 0.0, param1 = 0.75, param2 = 0.5, param3 = 0.5, param4 = 0.5 },
	},
	{
		id = "low_pass",
		engine = "filter",
		label = "low pass",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 1.0, param1 = 0.22, param2 = 0.48, param3 = 0.0, param4 = 1.0 },
	},
	{
		id = "band_pass",
		engine = "filter",
		label = "band pass",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 1.0, param1 = 0.44, param2 = 0.82, param3 = 0.5, param4 = 1.0 },
	},
	{
		id = "high_pass",
		engine = "filter",
		label = "high pass",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 1.0, param1 = 0.28, param2 = 0.42, param3 = 1.0, param4 = 1.0 },
	},
	{
		id = "muffled_bloom",
		engine = "filter",
		label = "muffled bloom",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.82, param1 = 0.24, param2 = 0.42, param3 = 0.0, param4 = 1.0 },
	},
	{
		id = "radio_tunnel",
		engine = "filter",
		label = "radio tunnel",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.84, param1 = 0.44, param2 = 0.74, param3 = 0.5, param4 = 1.0 },
	},
	{
		id = "airlift",
		engine = "filter",
		label = "airlift",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.72, param1 = 0.62, param2 = 0.3, param3 = 1.0, param4 = 1.0 },
	},
	{
		id = "peak_sweep",
		engine = "filter",
		label = "peak sweep",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.76, param1 = 0.58, param2 = 0.52, param3 = 0.5, param4 = 0.35 },
	},
	{
		id = "dark_shelf",
		engine = "filter",
		label = "dark shelf",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.7, param1 = 0.18, param2 = 0.22, param3 = 0.0, param4 = 0.0 },
	},
	{
		id = "thin_ice",
		engine = "filter",
		label = "thin ice",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.78, param1 = 0.74, param2 = 0.25, param3 = 1.0, param4 = 1.0 },
	},
	{
		id = "sub_cut",
		engine = "filter",
		label = "sub cut",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.64, param1 = 0.14, param2 = 0.18, param3 = 1.0, param4 = 0.0 },
	},
	{
		id = "vowel_sweep",
		engine = "filter",
		label = "vowel sweep",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.88, param1 = 0.5, param2 = 0.82, param3 = 0.5, param4 = 0.55 },
	},
	{
		id = "needle_focus",
		engine = "filter",
		label = "needle focus",
		params = { "cutoff", "res", "mode", "slope" },
		param_count = 4,
		defaults = { amount = 0.82, param1 = 0.68, param2 = 0.7, param3 = 0.5, param4 = 1.0 },
	},
	{
		id = "phaser",
		engine = "mod",
		label = "phaser",
		params = { "rate", "depth", "type", "stages" },
		param_count = 4,
		defaults = { amount = 0.55, param1 = 0.22, param2 = 0.42, param3 = 0.0, param4 = 0.58 },
	},
	{
		id = "flanger",
		engine = "mod",
		label = "flanger",
		params = { "rate", "depth", "type", "stages" },
		param_count = 4,
		defaults = { amount = 0.58, param1 = 0.28, param2 = 0.6, param3 = 0.5, param4 = 0.45 },
	},
	{
		id = "hollow_phase",
		engine = "mod",
		label = "hollow phase",
		params = { "rate", "depth", "type", "stages" },
		param_count = 4,
		defaults = { amount = 0.66, param1 = 0.16, param2 = 0.58, param3 = 0.0, param4 = 1.0 },
	},
	{
		id = "jet_wash",
		engine = "mod",
		label = "jet wash",
		params = { "rate", "depth", "type", "stages" },
		param_count = 4,
		defaults = { amount = 0.72, param1 = 0.24, param2 = 0.82, param3 = 0.5, param4 = 0.52 },
	},
	{
		id = "stereo_bloom",
		engine = "mod",
		label = "stereo bloom",
		params = { "rate", "depth", "type", "stages" },
		param_count = 4,
		defaults = { amount = 0.52, param1 = 0.18, param2 = 0.36, param3 = 1.0, param4 = 0.86 },
	},
	{
		id = "air_widen",
		engine = "space",
		label = "air widen",
		params = { "size", "damp", "type", "width" },
		param_count = 4,
		defaults = { amount = 0.36, param1 = 0.12, param2 = 0.18, param3 = 0.0, param4 = 0.95 },
	},
	{
		id = "washed_hall",
		engine = "space",
		label = "washed hall",
		params = { "size", "damp", "type", "width" },
		param_count = 4,
		defaults = { amount = 0.58, param1 = 0.72, param2 = 0.38, param3 = 1.0, param4 = 0.75 },
	},
	{
		id = "spring_tunnel",
		engine = "space",
		label = "spring tunnel",
		params = { "size", "damp", "type", "width" },
		param_count = 4,
		defaults = { amount = 0.55, param1 = 0.5, param2 = 0.35, param3 = 0.66, param4 = 0.35 },
	},
	{
		id = "burn_echo",
		engine = "delay",
		label = "burn echo",
		params = { "time", "feedback", "freeze", "tone" },
		param_count = 4,
		defaults = { amount = 0.62, param1 = 0.42, param2 = 0.62, param3 = 0.0, param4 = 0.3 },
	},
	{
		id = "compressor",
		engine = "texture",
		label = "compressor",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.46, param1 = 0.38, param2 = 0.36, param3 = 0.25, param4 = 0.2 },
	},
	{
		id = "limiter",
		engine = "texture",
		label = "limiter",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.58, param1 = 0.52, param2 = 0.18, param3 = 0.5, param4 = 0.0 },
	},
	{
		id = "glue_pump",
		engine = "texture",
		label = "glue pump",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.62, param1 = 0.34, param2 = 0.55, param3 = 0.25, param4 = 0.38 },
	},
	{
		id = "brick_squeeze",
		engine = "texture",
		label = "brick squeeze",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.78, param1 = 0.58, param2 = 0.16, param3 = 0.5, param4 = 0.0 },
	},
	{
		id = "crushed_bloom",
		engine = "texture",
		label = "crushed bloom",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.7, param1 = 0.4, param2 = 0.6, param3 = 0.75, param4 = 0.18 },
	},
	{
		id = "metal_gate",
		engine = "texture",
		label = "metal gate",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.66, param1 = 0.72, param2 = 0.46, param3 = 1.0, param4 = 0.52 },
	},
	{
		id = "broken_tape",
		engine = "texture",
		label = "broken tape",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.56, param1 = 0.28, param2 = 0.55, param3 = 0.75, param4 = 0.3 },
	},
	{
		id = "metal_room",
		engine = "texture",
		label = "metal room",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.52, param1 = 0.52, param2 = 0.48, param3 = 1.0, param4 = 0.4 },
	},
	{
		id = "karplus_bloom",
		engine = "resonator",
		label = "karplus bloom",
		params = { "tune", "decay", "body", "tone" },
		param_count = 4,
		defaults = { amount = 0.62, param1 = 0.24, param2 = 0.58, param3 = 0.48, param4 = 0.45 },
	},
	{
		id = "metal_string",
		engine = "resonator",
		label = "metal string",
		params = { "tune", "decay", "body", "tone" },
		param_count = 4,
		defaults = { amount = 0.72, param1 = 0.18, param2 = 0.74, param3 = 0.62, param4 = 0.3 },
	},
	{
		id = "folded_resonance",
		engine = "fold_filter",
		label = "folded resonance",
		params = { "cutoff", "res", "mode", "fold" },
		param_count = 4,
		defaults = { amount = 0.74, param1 = 0.42, param2 = 0.72, param3 = 0.5, param4 = 0.54 },
	},
	{
		id = "acid_fold",
		engine = "fold_filter",
		label = "acid fold",
		params = { "cutoff", "res", "mode", "fold" },
		param_count = 4,
		defaults = { amount = 0.82, param1 = 0.58, param2 = 0.88, param3 = 0.0, param4 = 0.78 },
	},
	{
		id = "vowel_formant",
		engine = "formant",
		label = "vowel formant",
		params = { "vowel", "spread", "focus", "tone" },
		param_count = 4,
		defaults = { amount = 0.64, param1 = 0.38, param2 = 0.42, param3 = 0.62, param4 = 0.55 },
	},
	{
		id = "choir_formant",
		engine = "formant",
		label = "choir formant",
		params = { "vowel", "spread", "focus", "tone" },
		param_count = 4,
		defaults = { amount = 0.72, param1 = 0.56, param2 = 0.58, param3 = 0.46, param4 = 0.7 },
	},
	{
		id = "plate_bright",
		engine = "plate",
		label = "plate bright",
		params = { "size", "decay", "pre", "tone" },
		param_count = 4,
		defaults = { amount = 0.58, param1 = 0.54, param2 = 0.48, param3 = 0.18, param4 = 0.82 },
	},
	{
		id = "plate_bloom",
		engine = "plate",
		label = "plate bloom",
		params = { "size", "decay", "pre", "tone" },
		param_count = 4,
		defaults = { amount = 0.74, param1 = 0.72, param2 = 0.66, param3 = 0.32, param4 = 0.58 },
	},
	{
		id = "early_bounce",
		engine = "early",
		label = "early bounce",
		params = { "size", "spread", "cluster", "tone" },
		param_count = 4,
		defaults = { amount = 0.52, param1 = 0.34, param2 = 0.48, param3 = 0.42, param4 = 0.72 },
	},
	{
		id = "room_slap",
		engine = "early",
		label = "room slap",
		params = { "size", "spread", "cluster", "tone" },
		param_count = 4,
		defaults = { amount = 0.66, param1 = 0.18, param2 = 0.64, param3 = 0.74, param4 = 0.52 },
	},
	{
		id = "haas_widen",
		engine = "haas",
		label = "haas widen",
		params = { "time", "spread", "tilt", "tone" },
		param_count = 4,
		defaults = { amount = 0.48, param1 = 0.42, param2 = 0.74, param3 = 0.5, param4 = 0.78 },
	},
	{
		id = "mono_lift",
		engine = "haas",
		label = "mono lift",
		params = { "time", "spread", "tilt", "tone" },
		param_count = 4,
		defaults = { amount = 0.58, param1 = 0.24, param2 = 0.9, param3 = 0.36, param4 = 0.64 },
	},
	{
		id = "tape_sat",
		engine = "tape_sat",
		label = "tape sat",
		params = { "drive", "bias", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.52, param1 = 0.38, param2 = 0.24, param3 = 0.58, param4 = 0.46 },
	},
	{
		id = "warm_glue",
		engine = "tape_sat",
		label = "warm glue",
		params = { "drive", "bias", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.68, param1 = 0.56, param2 = 0.18, param3 = 0.74, param4 = 0.34 },
	},
	{
		id = "transient_tame",
		engine = "transient",
		label = "transient tame",
		params = { "attack", "sustain", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.58, param1 = 0.72, param2 = 0.22, param3 = 0.7, param4 = 0.58 },
	},
	{
		id = "sustain_bloom",
		engine = "transient",
		label = "sustain bloom",
		params = { "attack", "sustain", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.72, param1 = 0.24, param2 = 0.82, param3 = 0.78, param4 = 0.52 },
	},
	{
		id = "tri_clip",
		engine = "multiclip",
		label = "tri clip",
		params = { "drive", "tilt", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.62, param1 = 0.52, param2 = 0.5, param3 = 0.68, param4 = 0.44 },
	},
	{
		id = "band_burn",
		engine = "multiclip",
		label = "band burn",
		params = { "drive", "tilt", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.78, param1 = 0.74, param2 = 0.66, param3 = 0.84, param4 = 0.28 },
	},
	{
		id = "tremolo",
		engine = "tremolo",
		label = "tremolo",
		params = { "rate", "depth", "shape", "bias" },
		param_count = 4,
		defaults = { amount = 0.58, param1 = 0.24, param2 = 0.72, param3 = 0.0, param4 = 0.5 },
	},
	{
		id = "gate_chop",
		engine = "tremolo",
		label = "gate chop",
		params = { "rate", "depth", "shape", "bias" },
		param_count = 4,
		defaults = { amount = 0.74, param1 = 0.36, param2 = 0.92, param3 = 1.0, param4 = 0.38 },
	},
	{
		id = "bit_crush",
		engine = "crusher",
		label = "bit crush",
		params = { "rate", "bits", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.6, param1 = 0.46, param2 = 0.42, param3 = 0.55, param4 = 0.42 },
	},
	{
		id = "data_melt",
		engine = "crusher",
		label = "data melt",
		params = { "rate", "bits", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.78, param1 = 0.18, param2 = 0.22, param3 = 0.84, param4 = 0.24 },
	},
	{
		id = "freq_shift",
		engine = "freq_shift",
		label = "freq shift",
		params = { "shift", "spread", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.46, param1 = 0.22, param2 = 0.18, param3 = 0.55, param4 = 0.62 },
	},
	{
		id = "robot_drift",
		engine = "freq_shift",
		label = "robot drift",
		params = { "shift", "spread", "mix", "tone" },
		param_count = 4,
		defaults = { amount = 0.72, param1 = 0.58, param2 = 0.34, param3 = 0.82, param4 = 0.36 },
	},
	{
		id = "grain_freeze",
		engine = "granular",
		label = "grain freeze",
		params = { "density", "size", "scatter", "tone" },
		param_count = 4,
		defaults = { amount = 0.92, param1 = 0.18, param2 = 0.78, param3 = 0.08, param4 = 0.46 },
	},
	{
		id = "grain_scatter",
		engine = "granular",
		label = "grain scatter",
		params = { "density", "size", "scatter", "tone" },
		param_count = 4,
		defaults = { amount = 1.0, param1 = 0.82, param2 = 0.22, param3 = 0.92, param4 = 0.34 },
	},
	{
		id = "comb_drift",
		engine = "mod",
		label = "comb drift",
		params = { "rate", "depth", "type", "stages" },
		param_count = 4,
		defaults = { amount = 0.48, param1 = 0.24, param2 = 0.35, param3 = 1.0, param4 = 0.72 },
	},
	{
		id = "freeze_haze",
		engine = "delay",
		label = "freeze haze",
		params = { "time", "feedback", "freeze", "tone" },
		param_count = 4,
		defaults = { amount = 0.58, param1 = 0.22, param2 = 0.78, param3 = 1.0, param4 = 0.36 },
	},
	{
		id = "dub_bloom",
		engine = "delay",
		label = "dub bloom",
		params = { "time", "feedback", "freeze", "tone" },
		param_count = 4,
		defaults = { amount = 0.6, param1 = 0.48, param2 = 0.68, param3 = 0.25, param4 = 0.42 },
	},
	{
		id = "glass_bite",
		engine = "texture",
		label = "glass bite",
		params = { "color", "damage", "type", "motion" },
		param_count = 4,
		defaults = { amount = 0.5, param1 = 0.82, param2 = 0.34, param3 = 1.0, param4 = 0.18 },
	},
	{
		id = "pressure_drive",
		engine = "eq",
		label = "pressure drive",
		params = { "freq", "gain", "q", "style" },
		param_count = 4,
		defaults = { amount = 0.54, param1 = 0.38, param2 = 0.7, param3 = 0.55, param4 = 0.0 },
	},
}

local effect_order = {}
local effect_labels = {}
local effect_params = {}
local effect_param_counts = {}
local default_values = {}
local effect_index_map = {}
local effect_engine_map = {}

-- Filter presets that share cutoff/res/slope when switching within the group.
local linked_filter_modes = {
	low_pass = true,
	band_pass = true,
	high_pass = true,
}

-- Maps old engine-name strings saved in v1 banks to current effect IDs.
local legacy_effect_map = {
	thru = "thru",
	filter = "washed_hall",
	eq = "pressure_drive",
	mod = "comb_drift",
	space = "washed_hall",
	texture = "broken_tape",
	delay = "dub_bloom",
}

-- Engine-side effect index used by the SC SynthDef SelectX.
local engine_effect_index = {
	thru = 0,
	filter = 1,
	eq = 2,
	mod = 3,
	space = 4,
	texture = 5,
	delay = 6,
	resonator = 7,
	fold_filter = 8,
	formant = 9,
	tremolo = 10,
	crusher = 11,
	freq_shift = 12,
	granular = 13,
	plate = 14,
	early = 15,
	haas = 16,
	tape_sat = 17,
	transient = 18,
	multiclip = 19,
}

for i, def in ipairs(effect_definitions) do
	effect_order[i] = def.id
	effect_labels[def.id] = def.label
	effect_params[def.id] = def.params
	effect_param_counts[def.id] = def.param_count
	default_values[def.id] = def.defaults
	effect_index_map[def.id] = i
	effect_engine_map[def.id] = def.engine
end

local NUM_SCENES = 16
local data_dir = _path.data .. "fadddddddder/"
local bank_file = data_dir .. "scene_bank.data"

local redraw_metro = nil
local engine_ready = false
local k1_down = false
local k1_used_as_modifier = false
local save_pending = false -- debounce flag for disk saves

local linlin = util.linlin
local clamp = util.clamp

local state = {
	page_index = 1,
	cursor = 1,
	xfade = 0.0,
	slots = { A = 1, B = 1 },
	bank = { A = {}, B = {} },
}

-- ---------------------------------------------------------------------------
-- Scene Bank
-- ---------------------------------------------------------------------------

local function clone_values(v)
	return { amount = v.amount, param1 = v.param1, param2 = v.param2, param3 = v.param3, param4 = v.param4 }
end

local function default_effect_values()
	local values = {}
	for _, id in ipairs(effect_order) do
		values[id] = clone_values(default_values[id])
	end
	return values
end

local sanitize_slot

local function new_scene_slot(index)
	return {
		effect = index == 1 and "dub_bloom" or "thru",
		values = default_effect_values(),
	}
end

local function clone_slot(slot, index)
	-- sanitize_slot returns a fresh slot for nil/non-table inputs.
	slot = sanitize_slot(slot, index)
	local out = { effect = slot.effect, values = {} }
	for _, id in ipairs(effect_order) do
		out.values[id] = clone_values(slot.values[id])
	end
	return out
end

sanitize_slot = function(slot, index)
	-- Reject anything that isn't a plain table (nil, strings, numbers, etc).
	if type(slot) ~= "table" then
		return new_scene_slot(index)
	end

	-- Migrate old engine-name strings saved in v1 format.
	if type(slot.effect) ~= "string" then
		slot.effect = nil
	end
	if slot.effect and legacy_effect_map[slot.effect] then
		slot.effect = legacy_effect_map[slot.effect]
	end
	if not effect_index_map[slot.effect] then
		slot.effect = index == 1 and "dub_bloom" or "thru"
	end

	-- values must be a plain table; anything else (string, nil, …) gets reset.
	if type(slot.values) ~= "table" then
		slot.values = default_effect_values()
		return slot
	end

	local loop_ok = pcall(function()
		for _, id in ipairs(effect_order) do
			local defaults = default_values[id]
			local v = slot.values[id]
			if type(v) ~= "table" then
				slot.values[id] = clone_values(defaults)
			else
				v.amount = clamp(tonumber(v.amount) or defaults.amount, 0, 1)
				v.param1 = clamp(tonumber(v.param1) or defaults.param1, 0, 1)
				v.param2 = clamp(tonumber(v.param2) or defaults.param2, 0, 1)
				v.param3 = clamp(tonumber(v.param3) or defaults.param3, 0, 1)
				v.param4 = clamp(tonumber(v.param4) or defaults.param4, 0, 1)
			end
		end
	end)
	if not loop_ok then
		slot.values = default_effect_values()
	end
	return slot
end

local function ensure_bank()
	if type(state.bank) ~= "table" then
		state.bank = { A = {}, B = {} }
	end

	-- Detect old v1 flat-array format: bank exists but bank.A does not.
	-- In that case duplicate all slots into both lanes.
	local a_is_table = type(state.bank.A) == "table"
	local b_is_table = type(state.bank.B) == "table"
	if not a_is_table or not b_is_table then
		local shared = state.bank
		state.bank = { A = {}, B = {} }
		for i = 1, NUM_SCENES do
			-- shared may be nil or garbage from any format; clone_slot →
			-- sanitize_slot handles all cases.
			local src = type(shared[i]) == "table" and shared[i] or nil
			state.bank.A[i] = clone_slot(src, i)
			state.bank.B[i] = clone_slot(src, i)
		end
	end

	-- Final pass: sanitize every slot in both lanes.
	for i = 1, NUM_SCENES do
		state.bank.A[i] = sanitize_slot(state.bank.A[i], i)
		state.bank.B[i] = sanitize_slot(state.bank.B[i], i)
	end

	state.slots.A = clamp(tonumber(state.slots.A) or 1, 1, NUM_SCENES)
	state.slots.B = clamp(tonumber(state.slots.B) or 1, 1, NUM_SCENES)
	state.xfade = clamp(tonumber(state.xfade) or 0, 0, 1)
end

local function save_bank()
	tabutil.save({ version = 2, slots = state.slots, xfade = state.xfade, bank = state.bank }, bank_file)
	save_pending = false
end

-- Queue a save; the metro will flush it next tick so rapid encoder moves
-- don't hammer the filesystem.
local function request_save()
	save_pending = true
end

local function load_bank()
	os.execute("mkdir -p " .. data_dir)
	local ok, data = pcall(tabutil.load, bank_file)
	if ok and type(data) == "table" then
		state.slots = type(data.slots) == "table" and data.slots or state.slots
		state.xfade = data.xfade or state.xfade
		state.bank = type(data.bank) == "table" and data.bank or state.bank
	end
	local ok2, err = pcall(ensure_bank)
	if not ok2 then
		print("fadddddddder: bank load failed (" .. tostring(err) .. "), resetting to defaults")
		os.remove(bank_file)
		state.bank = { A = {}, B = {} }
		state.slots = { A = 1, B = 1 }
		state.xfade = 0.0
		ensure_bank()
	end
	save_bank()
end

-- ---------------------------------------------------------------------------
-- Scene Helpers
-- ---------------------------------------------------------------------------

local function scene_for_side(side)
	if type(state.bank) ~= "table" or type(state.bank[side]) ~= "table" then
		ensure_bank()
	end
	local slot_index = clamp(tonumber(state.slots[side]) or 1, 1, NUM_SCENES)
	local scene = state.bank[side][slot_index]
	if type(scene) ~= "table" then
		scene = sanitize_slot(scene, slot_index)
		state.bank[side][slot_index] = scene
	end
	return scene
end

local function values_for_scene(scene)
	if type(scene) ~= "table" then
		scene = new_scene_slot(1)
	end
	if type(scene.values) ~= "table" then
		scene.values = default_effect_values()
	end
	if type(scene.values[scene.effect]) ~= "table" then
		scene.values[scene.effect] = clone_values(default_values[scene.effect] or default_values.thru)
	end
	return scene.values[scene.effect]
end

local function slot_label(side)
	return string.format("%s%02d", side, state.slots[side])
end

local function cursor_max_for_scene(scene)
	return 2 + (effect_param_counts[scene.effect] or 4)
end

-- ---------------------------------------------------------------------------
-- Engine Sync
-- ---------------------------------------------------------------------------

local function effect_engine_index_for(name)
	return engine_effect_index[effect_engine_map[name] or "thru"] or 0
end

local function sync_scene(side)
	if not engine.set_scene_a_effect then
		return
	end
	local scene = scene_for_side(side)
	local values = values_for_scene(scene)
	local idx = effect_engine_index_for(scene.effect)

	if side == "A" then
		engine.set_scene_a_effect(idx)
		engine.set_scene_a_amount(values.amount)
		engine.set_scene_a_param1(values.param1)
		engine.set_scene_a_param2(values.param2)
		engine.set_scene_a_param3(values.param3)
		engine.set_scene_a_param4(values.param4)
	else
		engine.set_scene_b_effect(idx)
		engine.set_scene_b_amount(values.amount)
		engine.set_scene_b_param1(values.param1)
		engine.set_scene_b_param2(values.param2)
		engine.set_scene_b_param3(values.param3)
		engine.set_scene_b_param4(values.param4)
	end
end

local function apply_bundle()
	if not engine_ready then
		return
	end
	sync_scene("A")
	sync_scene("B")
	engine.set_xfade(state.xfade)
end

local function apply_global_engine_params()
	if not engine_ready then
		return
	end
	engine.set_bpm(params:get("delay_bpm"))
	engine.set_delay_sync(params:get("delay_sync") == 2 and 1 or 0)
end

-- ---------------------------------------------------------------------------
-- Navigation / Editing
-- ---------------------------------------------------------------------------

local function current_page()
	return pages[state.page_index]
end

local function side_for_page(page)
	if page == "scene_a" then
		return "A"
	elseif page == "scene_b" then
		return "B"
	end
end

local function change_page(delta)
	state.page_index = clamp(state.page_index + delta, 1, #pages)
	state.cursor = 1
end

local function set_slot(side, slot)
	state.slots[side] = clamp(slot, 1, NUM_SCENES)
	state.cursor = 1
	apply_bundle()
	request_save()
end

local function adjust_scene(side, d)
	local scene = scene_for_side(side)
	local values = values_for_scene(scene)

	if state.cursor == 1 then
		local prev_effect = scene.effect
		local idx = clamp(effect_index_map[scene.effect] + d, 1, #effect_order)
		scene.effect = effect_order[idx]

		-- When staying within the linked filter group, carry over shared params
		-- so the transition sounds smooth; only reset the mode selector (param3).
		if linked_filter_modes[prev_effect] and linked_filter_modes[scene.effect] then
			local next_values = scene.values[scene.effect]
			next_values.amount = values.amount
			next_values.param1 = values.param1
			next_values.param2 = values.param2
			next_values.param4 = values.param4
			next_values.param3 = default_values[scene.effect].param3
		end

		-- Clamp cursor in case the new effect has fewer params.
		state.cursor = clamp(state.cursor, 1, cursor_max_for_scene(scene))
	elseif state.cursor == 2 then
		values.amount = clamp(values.amount + d * 0.02, 0, 1)
	elseif state.cursor == 3 then
		values.param1 = clamp(values.param1 + d * 0.02, 0, 1)
	elseif state.cursor == 4 then
		values.param2 = clamp(values.param2 + d * 0.02, 0, 1)
	elseif state.cursor == 5 then
		values.param3 = clamp(values.param3 + d * 0.02, 0, 1)
	else
		values.param4 = clamp(values.param4 + d * 0.02, 0, 1)
	end

	apply_bundle()
	request_save()
end

-- ---------------------------------------------------------------------------
-- Drawing
-- ---------------------------------------------------------------------------

-- 0–100 %, rounded.
local function pct(v)
	return string.format("%d%%", math.floor(v * 100 + 0.5))
end

-- Two-digit 0–99 compact value for the bottom-row split layout.
local function short_pct(v)
	return string.format("%02d", math.floor(v * 99 + 0.5))
end

local function draw_header(title)
	screen.level(5)
	screen.move(4, 8)
	screen.text("fadddddddder")
	screen.move(124, 8)
	screen.text_right(title)
end

local function draw_row(y, selected, label, value)
	screen.level(selected and 15 or 4)
	screen.move(6, y)
	screen.text(label)
	screen.move(122, y)
	screen.text_right(value)
end

local function draw_crossfader(y)
	local left_x = 20
	local right_x = 108
	local marker_x = linlin(0, 1, left_x, right_x, state.xfade)

	screen.level(3)
	screen.rect(left_x, y - 5, right_x - left_x, 10)
	screen.stroke()
	screen.level(6)
	screen.move(left_x + 4, y)
	screen.line(right_x - 4, y)
	screen.stroke()
	screen.level(15)
	screen.move(marker_x, y - 11)
	screen.line(marker_x, y + 11)
	screen.stroke()
	screen.level(6)
	screen.move(left_x, y + 16)
	screen.text(slot_label("A"))
	screen.move(right_x, y + 16)
	screen.text_right(slot_label("B"))
end

local function draw_perform_page()
	local scene_a = scene_for_side("A")
	local scene_b = scene_for_side("B")
	local values_a = values_for_scene(scene_a)
	local values_b = values_for_scene(scene_b)

	draw_header(page_labels.perform)
	draw_crossfader(32)

	screen.level(15)
	screen.move(6, 55)
	screen.text(effect_labels[scene_a.effect])
	screen.move(122, 55)
	screen.text_right(effect_labels[scene_b.effect])
	screen.level(6)
	screen.move(6, 64)
	screen.text(pct(values_a.amount))
	screen.move(122, 64)
	screen.text_right(pct(values_b.amount))
end

local function draw_scene_page(side)
	local scene = scene_for_side(side)
	local values = values_for_scene(scene)
	local param_labels = effect_params[scene.effect]
	local title = string.format("%s %02d", page_labels[current_page()], state.slots[side])

	draw_header(title)
	draw_row(18, state.cursor == 1, "effect", effect_labels[scene.effect])
	draw_row(28, state.cursor == 2, "amount", pct(values.amount))
	draw_row(38, state.cursor == 3, param_labels[1], pct(values.param1))
	draw_row(48, state.cursor == 4, param_labels[2], pct(values.param2))

	if (effect_param_counts[scene.effect] or 4) > 2 then
		screen.level(state.cursor == 5 and 15 or 4)
		screen.move(6, 62)
		screen.text(param_labels[3])
		screen.move(48, 62)
		screen.text(short_pct(values.param3))
		screen.level(state.cursor == 6 and 15 or 4)
		screen.move(74, 62)
		screen.text(param_labels[4])
		screen.move(122, 62)
		screen.text_right(short_pct(values.param4))
	end
end

-- ---------------------------------------------------------------------------
-- Norns Lifecycle
-- ---------------------------------------------------------------------------

function init()
	audio.level_adc(1.0)
	audio.level_dac(1.0)
	audio.level_monitor(1.0)

	params:add_separator("fadddddddder_delay", "fadddddddder delay")
	params:add_control("delay_bpm", "delay bpm", controlspec.new(40, 240, "lin", 1, 120, "bpm"))
	params:set_action("delay_bpm", function()
		apply_global_engine_params()
	end)
	params:add_option("delay_sync", "delay sync", { "free", "sync" }, 2)
	params:set_action("delay_sync", function()
		apply_global_engine_params()
	end)

	load_bank()

	-- Give the engine a moment to finish booting before sending commands.
	clock.run(function()
		clock.sleep(0.5)
		engine_ready = true
		engine.set_num_input_channels(2)
		engine.set_input_amp(1.0)
		engine.set_output_amp(1.0)
		apply_global_engine_params()
		apply_bundle()
	end)

	redraw_metro = metro.init()
	redraw_metro.time = 1 / 15
	redraw_metro.event = function()
		-- Flush any pending save before the next redraw frame.
		if save_pending then
			save_bank()
		end
		redraw()
	end
	redraw_metro:start()
end

function enc(n, d)
	local page = current_page()

	if n == 1 then
		change_page(d > 0 and 1 or -1)
	elseif n == 2 then
		if page == "perform" then
			if k1_down then
				k1_used_as_modifier = true
				set_slot("B", state.slots.B + d)
			else
				set_slot("A", state.slots.A + d)
			end
		else
			local side = side_for_page(page)
			state.cursor = clamp(state.cursor + d, 1, cursor_max_for_scene(scene_for_side(side)))
		end
	elseif n == 3 then
		if page == "perform" then
			state.xfade = clamp(state.xfade + d * 0.02, 0, 1)
			apply_bundle()
			request_save()
		else
			local side = side_for_page(page)
			if side then
				adjust_scene(side, d)
			end
		end
	end

	redraw()
end

function key(n, z)
	local page = current_page()

	if n == 1 then
		if z == 1 then
			k1_down = true
			k1_used_as_modifier = false
		else
			k1_down = false
			if not k1_used_as_modifier then
				-- Snap xfade to the canonical position for the current page.
				if page == "perform" then
					state.xfade = 0.5
				elseif page == "scene_a" then
					state.xfade = 0.0
				elseif page == "scene_b" then
					state.xfade = 1.0
				end
				apply_bundle()
				request_save()
			end
		end
	elseif z == 1 and n == 2 then
		change_page(-1)
	elseif z == 1 and n == 3 then
		change_page(1)
	end

	redraw()
end

function redraw()
	screen.clear()
	local page = current_page()
	if page == "perform" then
		draw_perform_page()
	elseif page == "scene_a" then
		draw_scene_page("A")
	elseif page == "scene_b" then
		draw_scene_page("B")
	end
	screen.update()
end

function cleanup()
	save_bank()
	if redraw_metro then
		redraw_metro:stop()
		redraw_metro = nil
	end
end
