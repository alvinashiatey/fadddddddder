-- fadddddddder: octo-inspired scene crossfader for live input
-- v0.6.0 @alvinashiatey

engine.name = "Fadddddddder"

local tabutil = require "tabutil"

local pages = { "perform", "scene_a", "scene_b" }
local page_labels = { perform = "perform", scene_a = "scene A", scene_b = "scene B" }

local effect_definitions = {
    { id = "thru",           engine = "thru",    label = "thru",           params = { "tone", "gain" },                      param_count = 2, defaults = { amount = 0.0, param1 = 0.75, param2 = 0.5,  param3 = 0.5,  param4 = 0.5 } },
    { id = "low_pass",       engine = "filter",  label = "low pass",       params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 1.0,  param1 = 0.22, param2 = 0.48, param3 = 0.0,  param4 = 1.0 } },
    { id = "band_pass",      engine = "filter",  label = "band pass",      params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 1.0,  param1 = 0.44, param2 = 0.82, param3 = 0.5,  param4 = 1.0 } },
    { id = "high_pass",      engine = "filter",  label = "high pass",      params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 1.0,  param1 = 0.28, param2 = 0.42, param3 = 1.0,  param4 = 1.0 } },
    { id = "muffled_bloom",  engine = "filter",  label = "muffled bloom",  params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.82, param1 = 0.24, param2 = 0.42, param3 = 0.0,  param4 = 1.0 } },
    { id = "radio_tunnel",   engine = "filter",  label = "radio tunnel",   params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.84, param1 = 0.44, param2 = 0.74, param3 = 0.5,  param4 = 1.0 } },
    { id = "airlift",        engine = "filter",  label = "airlift",        params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.72, param1 = 0.62, param2 = 0.3,  param3 = 1.0,  param4 = 1.0 } },
    { id = "peak_sweep",     engine = "filter",  label = "peak sweep",     params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.76, param1 = 0.58, param2 = 0.52, param3 = 0.5,  param4 = 0.35 } },
    { id = "dark_shelf",     engine = "filter",  label = "dark shelf",     params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.7,  param1 = 0.18, param2 = 0.22, param3 = 0.0,  param4 = 0.0 } },
    { id = "thin_ice",       engine = "filter",  label = "thin ice",       params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.78, param1 = 0.74, param2 = 0.25, param3 = 1.0,  param4 = 1.0 } },
    { id = "sub_cut",        engine = "filter",  label = "sub cut",        params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.64, param1 = 0.14, param2 = 0.18, param3 = 1.0,  param4 = 0.0 } },
    { id = "vowel_sweep",    engine = "filter",  label = "vowel sweep",    params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.88, param1 = 0.5,  param2 = 0.82, param3 = 0.5,  param4 = 0.55 } },
    { id = "needle_focus",   engine = "filter",  label = "needle focus",   params = { "cutoff", "res", "mode", "slope" },   param_count = 4, defaults = { amount = 0.82, param1 = 0.68, param2 = 0.7,  param3 = 0.5,  param4 = 1.0 } },
    { id = "phaser",         engine = "mod",     label = "phaser",         params = { "rate", "depth", "type", "stages" },   param_count = 4, defaults = { amount = 0.55, param1 = 0.22, param2 = 0.42, param3 = 0.0,  param4 = 0.58 } },
    { id = "flanger",        engine = "mod",     label = "flanger",        params = { "rate", "depth", "type", "stages" },   param_count = 4, defaults = { amount = 0.58, param1 = 0.28, param2 = 0.6,  param3 = 0.5,  param4 = 0.45 } },
    { id = "hollow_phase",   engine = "mod",     label = "hollow phase",   params = { "rate", "depth", "type", "stages" },   param_count = 4, defaults = { amount = 0.66, param1 = 0.16, param2 = 0.58, param3 = 0.0,  param4 = 1.0 } },
    { id = "jet_wash",       engine = "mod",     label = "jet wash",       params = { "rate", "depth", "type", "stages" },   param_count = 4, defaults = { amount = 0.72, param1 = 0.24, param2 = 0.82, param3 = 0.5,  param4 = 0.52 } },
    { id = "stereo_bloom",   engine = "mod",     label = "stereo bloom",   params = { "rate", "depth", "type", "stages" },   param_count = 4, defaults = { amount = 0.52, param1 = 0.18, param2 = 0.36, param3 = 1.0,  param4 = 0.86 } },
    { id = "air_widen",      engine = "space",   label = "air widen",      params = { "size", "damp", "type", "width" },  param_count = 4, defaults = { amount = 0.36, param1 = 0.12, param2 = 0.18, param3 = 0.0,  param4 = 0.95 } },
    { id = "washed_hall",    engine = "space",   label = "washed hall",    params = { "size", "damp", "type", "width" },  param_count = 4, defaults = { amount = 0.58, param1 = 0.72, param2 = 0.38, param3 = 1.0,  param4 = 0.75 } },
    { id = "spring_tunnel",  engine = "space",   label = "spring tunnel",  params = { "size", "damp", "type", "width" },  param_count = 4, defaults = { amount = 0.55, param1 = 0.5,  param2 = 0.35, param3 = 0.66, param4 = 0.35 } },
    { id = "burn_echo",      engine = "delay",   label = "burn echo",      params = { "time", "feedback", "freeze", "tone" }, param_count = 4, defaults = { amount = 0.62, param1 = 0.42, param2 = 0.62, param3 = 0.0,  param4 = 0.3 } },
    { id = "compressor",     engine = "texture", label = "compressor",     params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.46, param1 = 0.38, param2 = 0.36, param3 = 0.25, param4 = 0.2 } },
    { id = "limiter",        engine = "texture", label = "limiter",        params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.58, param1 = 0.52, param2 = 0.18, param3 = 0.5,  param4 = 0.0 } },
    { id = "glue_pump",      engine = "texture", label = "glue pump",      params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.62, param1 = 0.34, param2 = 0.55, param3 = 0.25, param4 = 0.38 } },
    { id = "brick_squeeze",  engine = "texture", label = "brick squeeze",  params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.78, param1 = 0.58, param2 = 0.16, param3 = 0.5,  param4 = 0.0 } },
    { id = "crushed_bloom",  engine = "texture", label = "crushed bloom",  params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.7,  param1 = 0.4,  param2 = 0.6,  param3 = 0.75, param4 = 0.18 } },
    { id = "metal_gate",     engine = "texture", label = "metal gate",     params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.66, param1 = 0.72, param2 = 0.46, param3 = 1.0,  param4 = 0.52 } },
    { id = "broken_tape",    engine = "texture", label = "broken tape",    params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.56, param1 = 0.28, param2 = 0.55, param3 = 0.75, param4 = 0.3 } },
    { id = "metal_room",     engine = "texture", label = "metal room",     params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.52, param1 = 0.52, param2 = 0.48, param3 = 1.0,  param4 = 0.4 } },
    { id = "comb_drift",     engine = "mod",     label = "comb drift",     params = { "rate", "depth", "type", "stages" },  param_count = 4, defaults = { amount = 0.48, param1 = 0.24, param2 = 0.35, param3 = 1.0,  param4 = 0.72 } },
    { id = "freeze_haze",    engine = "delay",   label = "freeze haze",    params = { "time", "feedback", "freeze", "tone" }, param_count = 4, defaults = { amount = 0.58, param1 = 0.22, param2 = 0.78, param3 = 1.0,  param4 = 0.36 } },
    { id = "dub_bloom",      engine = "delay",   label = "dub bloom",      params = { "time", "feedback", "freeze", "tone" }, param_count = 4, defaults = { amount = 0.6,  param1 = 0.48, param2 = 0.68, param3 = 0.25, param4 = 0.42 } },
    { id = "glass_bite",     engine = "texture", label = "glass bite",     params = { "color", "damage", "type", "motion" }, param_count = 4, defaults = { amount = 0.5,  param1 = 0.82, param2 = 0.34, param3 = 1.0,  param4 = 0.18 } },
    { id = "pressure_drive", engine = "eq",      label = "pressure drive", params = { "freq", "gain", "q", "style" },      param_count = 4, defaults = { amount = 0.54, param1 = 0.38, param2 = 0.7,  param3 = 0.55, param4 = 0.0 } },
}

local effect_order = {}
local effect_labels = {}
local effect_params = {}
local effect_param_counts = {}
local default_values = {}
local effect_index_map = {}
local effect_engine_map = {}
local legacy_effect_map = {
    thru = "thru",
    filter = "washed_hall",
    eq = "pressure_drive",
    mod = "comb_drift",
    space = "washed_hall",
    texture = "broken_tape",
    delay = "dub_bloom",
}

for i, definition in ipairs(effect_definitions) do
    effect_order[i] = definition.id
    effect_labels[definition.id] = definition.label
    effect_params[definition.id] = definition.params
    effect_param_counts[definition.id] = definition.param_count
    default_values[definition.id] = definition.defaults
    effect_index_map[definition.id] = i
    effect_engine_map[definition.id] = definition.engine
end

local NUM_SCENES = 16
local data_dir = _path.data .. "fadddddddder/"
local bank_file = data_dir .. "scene_bank.data"

local redraw_metro = nil
local engine_ready = false
local k1_down = false
local k1_used_as_modifier = false

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

local function clone_values(values)
    return {
        amount = values.amount,
        param1 = values.param1,
        param2 = values.param2,
        param3 = values.param3,
        param4 = values.param4,
    }
end

local function default_effect_values()
    local values = {}
    for _, effect in ipairs(effect_order) do values[effect] = clone_values(default_values[effect]) end
    return values
end

local sanitize_slot

local function new_scene_slot(index)
    local effect = index == 1 and "dub_bloom" or "thru"
    return {
        effect = effect,
        values = default_effect_values(),
    }
end

local function clone_slot(slot, index)
    slot = sanitize_slot(slot, index)
    local cloned = { effect = slot.effect, values = {} }
    for _, effect in ipairs(effect_order) do cloned.values[effect] = clone_values(slot.values[effect]) end
    return cloned
end

sanitize_slot = function(slot, index)
    if type(slot) ~= "table" then return new_scene_slot(index) end
    if legacy_effect_map[slot.effect] ~= nil then slot.effect = legacy_effect_map[slot.effect] end
    if effect_index_map[slot.effect] == nil then slot.effect = index == 1 and "dub_bloom" or "thru" end
    if type(slot.values) ~= "table" then slot.values = {} end
    for _, effect in ipairs(effect_order) do
        local defaults = default_values[effect]
        local values = slot.values[effect]
        if type(values) ~= "table" then
            slot.values[effect] = clone_values(defaults)
        else
            values.amount = clamp(tonumber(values.amount) or defaults.amount, 0, 1)
            values.param1 = clamp(tonumber(values.param1) or defaults.param1, 0, 1)
            values.param2 = clamp(tonumber(values.param2) or defaults.param2, 0, 1)
            values.param3 = clamp(tonumber(values.param3) or defaults.param3, 0, 1)
            values.param4 = clamp(tonumber(values.param4) or defaults.param4, 0, 1)
        end
    end
    return slot
end

local function ensure_bank()
    if type(state.bank) ~= "table" then state.bank = { A = {}, B = {} } end

    -- Migrate old shared-bank files by copying the old 1-16 slots into both lanes.
    if type(state.bank.A) ~= "table" or type(state.bank.B) ~= "table" then
        local shared_bank = state.bank
        state.bank = { A = {}, B = {} }
        for i = 1, NUM_SCENES do
            state.bank.A[i] = clone_slot(shared_bank[i], i)
            state.bank.B[i] = clone_slot(shared_bank[i], i)
        end
    end

    for i = 1, NUM_SCENES do
        state.bank.A[i] = sanitize_slot(state.bank.A[i], i)
        state.bank.B[i] = sanitize_slot(state.bank.B[i], i)
    end
    state.slots.A = clamp(tonumber(state.slots.A) or 1, 1, NUM_SCENES)
    state.slots.B = clamp(tonumber(state.slots.B) or 1, 1, NUM_SCENES)
    state.xfade = clamp(tonumber(state.xfade) or 0, 0, 1)
end

local function save_bank()
    os.execute("mkdir -p " .. data_dir)
    tabutil.save({ version = 2, slots = state.slots, xfade = state.xfade, bank = state.bank }, bank_file)
end

local function load_bank()
    os.execute("mkdir -p " .. data_dir)
    local ok, data = pcall(tabutil.load, bank_file)
    if ok and type(data) == "table" then
        state.slots = type(data.slots) == "table" and data.slots or state.slots
        state.xfade = data.xfade or state.xfade
        state.bank = type(data.bank) == "table" and data.bank or state.bank
    end
    ensure_bank()
    save_bank()
end

local function scene_for_side(side)
    return state.bank[side][state.slots[side]]
end

local function values_for_scene(scene)
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

local function effect_engine_index(name)
    local engine_effect = effect_engine_map[name] or "thru"
    local engine_order = { thru = 0, filter = 1, eq = 2, mod = 3, space = 4, texture = 5, delay = 6 }
    return engine_order[engine_effect] or 0
end

local function sync_scene(side)
    if engine.set_scene_a_effect == nil or engine.set_scene_b_effect == nil then return end
    local scene = scene_for_side(side)
    local values = values_for_scene(scene)

    if side == "A" then
        engine.set_scene_a_effect(effect_engine_index(scene.effect))
        engine.set_scene_a_amount(values.amount)
        engine.set_scene_a_param1(values.param1)
        engine.set_scene_a_param2(values.param2)
        engine.set_scene_a_param3(values.param3)
        engine.set_scene_a_param4(values.param4)
    else
        engine.set_scene_b_effect(effect_engine_index(scene.effect))
        engine.set_scene_b_amount(values.amount)
        engine.set_scene_b_param1(values.param1)
        engine.set_scene_b_param2(values.param2)
        engine.set_scene_b_param3(values.param3)
        engine.set_scene_b_param4(values.param4)
    end
end

local function apply_bundle()
    if not engine_ready then return end
    sync_scene("A")
    sync_scene("B")
    engine.set_xfade(state.xfade)
end

local function apply_global_engine_params()
    if not engine_ready then return end
    if engine.set_bpm ~= nil then engine.set_bpm(params:get("delay_bpm")) end
    if engine.set_delay_sync ~= nil then engine.set_delay_sync(params:get("delay_sync") == 2 and 1 or 0) end
end

-- ---------------------------------------------------------------------------
-- Navigation / Editing
-- ---------------------------------------------------------------------------

local function current_page()
    return pages[state.page_index]
end

local function side_for_page(page)
    if page == "scene_a" then return "A" end
    if page == "scene_b" then return "B" end
end

local function change_page(delta)
    state.page_index = clamp(state.page_index + delta, 1, #pages)
    state.cursor = 1
end

local function set_slot(side, slot)
    state.slots[side] = clamp(slot, 1, NUM_SCENES)
    state.cursor = 1
    apply_bundle()
    save_bank()
end

local function adjust_scene(side, d)
    local scene = scene_for_side(side)
    local values = values_for_scene(scene)

    if state.cursor == 1 then
        local idx = clamp(effect_index_map[scene.effect] + d, 1, #effect_order)
        scene.effect = effect_order[idx]
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
    save_bank()
end

-- ---------------------------------------------------------------------------
-- Drawing
-- ---------------------------------------------------------------------------

local function pct(v)
    return string.format("%d%%", math.floor(v * 100 + 0.5))
end

local function short_pct(v)
    return string.format("%02d", math.floor(v * 99 + 0.5))
end

local function draw_header(title)
    screen.level(5)
    screen.move(4, 8); screen.text("fadddddddder")
    screen.move(124, 8); screen.text_right(title)
end

local function draw_row(y, selected, label, value)
    screen.level(selected and 15 or 4)
    screen.move(6, y); screen.text(label)
    screen.move(122, y); screen.text_right(value)
end

local function draw_crossfader(y)
    local left_x = 20
    local right_x = 108
    local marker_x = linlin(0, 1, left_x, right_x, state.xfade)

    screen.level(3)
    screen.rect(left_x, y - 5, right_x - left_x, 10); screen.stroke()
    screen.level(6)
    screen.move(left_x + 4, y); screen.line(right_x - 4, y); screen.stroke()
    screen.level(15)
    screen.move(marker_x, y - 11); screen.line(marker_x, y + 11); screen.stroke()
    screen.level(6)
    screen.move(left_x, y + 16); screen.text(slot_label("A"))
    screen.move(right_x, y + 16); screen.text_right(slot_label("B"))
end

local function draw_perform_page()
    local scene_a = scene_for_side("A")
    local scene_b = scene_for_side("B")
    local values_a = values_for_scene(scene_a)
    local values_b = values_for_scene(scene_b)

    draw_header(page_labels.perform)
    draw_crossfader(32)

    screen.level(15)
    screen.move(6, 55); screen.text(effect_labels[scene_a.effect])
    screen.move(122, 55); screen.text_right(effect_labels[scene_b.effect])
    screen.level(6)
    screen.move(6, 64); screen.text(pct(values_a.amount))
    screen.move(122, 64); screen.text_right(pct(values_b.amount))
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
        screen.move(6, 62); screen.text(param_labels[3])
        screen.move(48, 62); screen.text(short_pct(values.param3))
        screen.level(state.cursor == 6 and 15 or 4)
        screen.move(74, 62); screen.text(param_labels[4])
        screen.move(122, 62); screen.text_right(short_pct(values.param4))
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
    redraw_metro.event = redraw
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
                set_slot("B", state.slots.B + d, d)
            else
                set_slot("A", state.slots.A + d, d)
            end
        else
            local side = side_for_page(page)
            state.cursor = clamp(state.cursor + d, 1, cursor_max_for_scene(scene_for_side(side)))
        end
    elseif n == 3 then
        if page == "perform" then
            state.xfade = clamp(state.xfade + d * 0.02, 0, 1)
            apply_bundle()
            save_bank()
        else
            local side = side_for_page(page)
            if side then adjust_scene(side, d) end
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
            if page == "perform" and not k1_used_as_modifier then
                state.xfade = 0.5
                apply_bundle()
                save_bank()
            elseif page == "scene_a" and not k1_used_as_modifier then
                state.xfade = 0.0
                apply_bundle()
                save_bank()
            elseif page == "scene_b" and not k1_used_as_modifier then
                state.xfade = 1.0
                apply_bundle()
                save_bank()
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
    if redraw_metro ~= nil then
        redraw_metro:stop()
        redraw_metro = nil
    end
end
