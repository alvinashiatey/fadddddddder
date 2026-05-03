-- fadddddddder: octo-inspired scene crossfader for live input
-- v0.5.0 @alvinashiatey

engine.name            = "Fadddddddder"

local ControlSpec      = require "controlspec"

local pages            = { "perform", "scene_a", "scene_b" }
local page_labels      = { perform = "perform", scene_a = "scene A", scene_b = "scene B" }

local effect_order     = { "thru", "filter", "eq", "mod", "space", "texture", "delay" }
local effect_labels    = {
    thru    = "thru",
    filter  = "filter",
    eq      = "eq",
    mod     = "mod",
    space   = "space",
    texture = "texture",
    delay   = "delay",
}

local effect_options   = {}

local effect_params    = {
    thru    = { "tone", "gain" },
    filter  = { "cutoff", "res", "mode", "slope" },
    eq      = { "freq", "gain", "q", "style" },
    mod     = { "rate", "depth", "type", "stages" },
    space   = { "size", "damp", "type", "width" },
    texture = { "color", "damage", "type", "motion" },
    delay   = { "time", "feedback", "freeze", "tone" },
}

local effect_param_counts = {
    thru    = 2,
    filter  = 4,
    eq      = 4,
    mod     = 4,
    space   = 4,
    texture = 4,
    delay   = 4,
}

local effect_index_map = {}
for i, name in ipairs(effect_order) do
    effect_index_map[name] = i
    effect_options[i] = effect_labels[name]
end

local redraw_metro = nil
local engine_ready = false

local state        = {
    page_index = 1,
    cursor     = 1,
    xfade      = 0.0,
    scenes     = {
        A = { effect = "thru", amount = 0.0, param1 = 0.5, param2 = 0.5, param3 = 0.5, param4 = 0.5 },
        B = { effect = "delay", amount = 0.55, param1 = 0.42, param2 = 0.65, param3 = 0.0, param4 = 0.45 },
    },
}

local linlin       = util.linlin
local clamp        = util.clamp
local pct_spec     = ControlSpec.new(0, 1, "lin", 0.01, 0, "")
local cursor_max_for_scene

-- ---------------------------------------------------------------------------
-- Engine sync
-- ---------------------------------------------------------------------------

local function effect_engine_index(name)
    -- Engine expects 0-based index
    return (effect_index_map[name] or 1) - 1
end

local function sync_scene(scene_name)
    local scene = state.scenes[scene_name]
    if scene_name == "A" then
        engine.set_scene_a_effect(effect_engine_index(scene.effect))
        engine.set_scene_a_amount(scene.amount)
        engine.set_scene_a_param1(scene.param1)
        engine.set_scene_a_param2(scene.param2)
        engine.set_scene_a_param3(scene.param3)
        engine.set_scene_a_param4(scene.param4)
    else
        engine.set_scene_b_effect(effect_engine_index(scene.effect))
        engine.set_scene_b_amount(scene.amount)
        engine.set_scene_b_param1(scene.param1)
        engine.set_scene_b_param2(scene.param2)
        engine.set_scene_b_param3(scene.param3)
        engine.set_scene_b_param4(scene.param4)
    end
end

local function apply_bundle()
    if not engine_ready then return end
    sync_scene("A")
    sync_scene("B")
    engine.set_xfade(state.xfade)
end

local function scene_param_prefix(scene_name)
    return scene_name == "A" and "scene_a" or "scene_b"
end

local function set_scene_from_params(scene_name)
    local scene = state.scenes[scene_name]
    local prefix = scene_param_prefix(scene_name)
    scene.effect = effect_order[params:get(prefix .. "_effect")]
    scene.amount = params:get(prefix .. "_amount")
    scene.param1 = params:get(prefix .. "_param1")
    scene.param2 = params:get(prefix .. "_param2")
    scene.param3 = params:get(prefix .. "_param3")
    scene.param4 = params:get(prefix .. "_param4")
end

local function add_scene_params(scene_name)
    local prefix = scene_param_prefix(scene_name)
    local label = scene_name == "A" and "Scene A" or "Scene B"
    local scene = state.scenes[scene_name]

    params:add_separator(label)
    params:add_option(prefix .. "_effect", label .. " Effect", effect_options, effect_index_map[scene.effect])
    params:set_action(prefix .. "_effect", function(value)
        state.scenes[scene_name].effect = effect_order[value]
        state.cursor = clamp(state.cursor, 1, cursor_max_for_scene(state.scenes[scene_name]))
        apply_bundle()
        redraw()
    end)

    params:add_control(prefix .. "_amount", label .. " Amount", pct_spec)
    params:set(prefix .. "_amount", scene.amount)
    params:set_action(prefix .. "_amount", function(value)
        state.scenes[scene_name].amount = value
        apply_bundle()
        redraw()
    end)

    params:add_control(prefix .. "_param1", label .. " Macro 1", pct_spec)
    params:set(prefix .. "_param1", scene.param1)
    params:set_action(prefix .. "_param1", function(value)
        state.scenes[scene_name].param1 = value
        apply_bundle()
        redraw()
    end)

    params:add_control(prefix .. "_param2", label .. " Macro 2", pct_spec)
    params:set(prefix .. "_param2", scene.param2)
    params:set_action(prefix .. "_param2", function(value)
        state.scenes[scene_name].param2 = value
        apply_bundle()
        redraw()
    end)

    params:add_control(prefix .. "_param3", label .. " Macro 3", pct_spec)
    params:set(prefix .. "_param3", scene.param3)
    params:set_action(prefix .. "_param3", function(value)
        state.scenes[scene_name].param3 = value
        apply_bundle()
        redraw()
    end)

    params:add_control(prefix .. "_param4", label .. " Macro 4", pct_spec)
    params:set(prefix .. "_param4", scene.param4)
    params:set_action(prefix .. "_param4", function(value)
        state.scenes[scene_name].param4 = value
        apply_bundle()
        redraw()
    end)
end

local function add_params()
    params:add_separator("fadddddddder")
    params:add_control("xfade", "Crossfade", pct_spec)
    params:set("xfade", state.xfade)
    params:set_action("xfade", function(value)
        state.xfade = value
        apply_bundle()
        redraw()
    end)

    add_scene_params("A")
    add_scene_params("B")
end

-- ---------------------------------------------------------------------------
-- Navigation
-- ---------------------------------------------------------------------------

local function current_page()
    return pages[state.page_index]
end

local function scene_for_page(page)
    if page == "scene_a" then
        return "A"
    elseif page == "scene_b" then
        return "B"
    end
end

local function change_page(delta)
    state.page_index = clamp(state.page_index + delta, 1, #pages)
    state.cursor     = 1
end

local function adjust_scene(scene_name, d)
    local scene = state.scenes[scene_name]
    local prefix = scene_param_prefix(scene_name)
    if state.cursor == 1 then
        local idx    = clamp(effect_index_map[scene.effect] + d, 1, #effect_order)
        params:set(prefix .. "_effect", idx)
    elseif state.cursor == 2 then
        params:set(prefix .. "_amount", clamp(scene.amount + d * 0.02, 0, 1))
    elseif state.cursor == 3 then
        params:set(prefix .. "_param1", clamp(scene.param1 + d * 0.02, 0, 1))
    elseif state.cursor == 4 then
        params:set(prefix .. "_param2", clamp(scene.param2 + d * 0.02, 0, 1))
    elseif state.cursor == 5 then
        params:set(prefix .. "_param3", clamp(scene.param3 + d * 0.02, 0, 1))
    else
        params:set(prefix .. "_param4", clamp(scene.param4 + d * 0.02, 0, 1))
    end
end

cursor_max_for_scene = function(scene)
    return 2 + (effect_param_counts[scene.effect] or 4)
end

-- ---------------------------------------------------------------------------
-- Drawing helpers
-- ---------------------------------------------------------------------------

local function draw_header(title)
    screen.level(5)
    screen.move(4, 8)
    screen.text("fadddddddder")
    screen.move(124, 8)
    screen.text_right(title)
end

local function draw_row(y, selected, label, value)
    screen.level(selected and 15 or 4)
    screen.move(6, y); screen.text(label)
    screen.move(122, y); screen.text_right(value)
end

local function draw_crossfader(y)
    local left_x   = 20
    local right_x  = 108
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
    screen.move(left_x, y + 16); screen.text("A")
    screen.move(right_x, y + 16); screen.text_right("B")
end

local function pct(v)
    return string.format("%d%%", math.floor(v * 100 + 0.5))
end

local function short_pct(v)
    return string.format("%02d", math.floor(v * 99 + 0.5))
end

local function draw_perform_page()
    draw_header(page_labels.perform)

    draw_crossfader(32)

    screen.level(15)
    screen.move(6, 55); screen.text(effect_labels[state.scenes.A.effect])
    screen.move(122, 55); screen.text_right(effect_labels[state.scenes.B.effect])
    screen.level(6)
    screen.move(6, 64); screen.text(pct(state.scenes.A.amount))
    screen.move(122, 64); screen.text_right(pct(state.scenes.B.amount))
end

local function draw_scene_page(scene_name)
    local scene = state.scenes[scene_name]
    local param_labels = effect_params[scene.effect]
    draw_header(page_labels[current_page()])

    draw_row(18, state.cursor == 1, "effect", effect_labels[scene.effect])
    draw_row(28, state.cursor == 2, "amount", pct(scene.amount))
    draw_row(38, state.cursor == 3, param_labels[1], pct(scene.param1))
    draw_row(48, state.cursor == 4, param_labels[2], pct(scene.param2))
    if (effect_param_counts[scene.effect] or 4) > 2 then
        screen.level(state.cursor == 5 and 15 or 4)
        screen.move(6, 62); screen.text(param_labels[3])
        screen.move(48, 62); screen.text(short_pct(scene.param3))
        screen.level(state.cursor == 6 and 15 or 4)
        screen.move(74, 62); screen.text(param_labels[4])
        screen.move(122, 62); screen.text_right(short_pct(scene.param4))
    end

end

-- ---------------------------------------------------------------------------
-- Norns lifecycle
-- ---------------------------------------------------------------------------

function init()
    add_params()
    params:bang()
    set_scene_from_params("A")
    set_scene_from_params("B")

    -- The engine's alloc (SynthDef compilation + Synth instantiation) runs
    -- asynchronously. Sending engine commands immediately in init() races
    -- against alloc completing and will hit nil synth references in SC.
    -- A short clock.sleep gives alloc time to finish before we push state.
    clock.run(function()
        clock.sleep(0.5)
        engine_ready = true
        engine.set_num_input_channels(2)
        engine.set_input_amp(1.0)
        engine.set_output_amp(1.0)
        apply_bundle()
    end)

    redraw_metro       = metro.init()
    redraw_metro.time  = 1 / 15
    redraw_metro.event = redraw
    redraw_metro:start()
end

function enc(n, d)
    local page = current_page()
    if n == 1 then
        change_page(d > 0 and 1 or -1)
    elseif n == 2 then
        if page ~= "perform" then
            local scene_name = scene_for_page(page)
            local scene = state.scenes[scene_name]
            state.cursor = clamp(state.cursor + d, 1, cursor_max_for_scene(scene))
        end
    elseif n == 3 then
        if page == "perform" then
            params:set("xfade", clamp(state.xfade + d * 0.02, 0, 1))
        else
            local scene_name = scene_for_page(page)
            if scene_name then adjust_scene(scene_name, d) end
        end
    end
    redraw()
end

function key(n, z)
    if z == 0 then return end
    local page = current_page()

    if n == 2 then
        change_page(-1)
    elseif n == 3 then
        change_page(1)
    elseif n == 1 then
        if page == "perform" then
            params:set("xfade", 0.5)
        elseif page == "scene_a" then
            params:set("xfade", 0.0)
        elseif page == "scene_b" then
            params:set("xfade", 1.0)
        end
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
    if redraw_metro ~= nil then
        redraw_metro:stop()
        redraw_metro = nil
    end
end
