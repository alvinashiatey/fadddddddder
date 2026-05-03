-- fadddddddder: octo-inspired scene crossfader for live input
-- v0.3.1 @alvinashiatey

engine.name            = "Fadddddddder"

local pages            = { "perform", "scene_a", "scene_b" }
local page_labels      = { perform = "perform", scene_a = "scene A", scene_b = "scene B" }

local effect_order     = { "thru", "lp", "hp", "dub", "micro", "freeze" }
local effect_labels    = {
    thru   = "thru",
    lp     = "lowpass",
    hp     = "highpass",
    dub    = "dub echo",
    micro  = "microloop",
    freeze = "freeze",
}

local effect_index_map = {}
for i, name in ipairs(effect_order) do effect_index_map[name] = i end

local redraw_metro = nil

local state        = {
    page_index = 1,
    cursor     = 1,
    xfade      = 0.0,
    scenes     = {
        A = { effect = "thru", amount = 0.0 },
        B = { effect = "dub", amount = 0.55 },
    },
}

local linlin       = util.linlin
local clamp        = util.clamp

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
    else
        engine.set_scene_b_effect(effect_engine_index(scene.effect))
        engine.set_scene_b_amount(scene.amount)
    end
end

local function apply_bundle()
    sync_scene("A")
    sync_scene("B")
    engine.set_xfade(state.xfade)
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
    state.page_index = ((state.page_index - 1 + delta) % #pages) + 1
    state.cursor     = 1
end

local function adjust_scene(scene_name, d)
    local scene = state.scenes[scene_name]
    if state.cursor == 1 then
        local idx    = clamp(effect_index_map[scene.effect] + d, 1, #effect_order)
        scene.effect = effect_order[idx]
    else
        scene.amount = clamp(scene.amount + d * 0.02, 0, 1)
    end
    apply_bundle()
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

local function draw_crossfader(y)
    local left_x   = 16
    local right_x  = 112
    local marker_x = linlin(0, 1, left_x, right_x, state.xfade)

    screen.level(15)
    screen.move(4, y); screen.text("[A]")
    screen.move(124, y); screen.text_right("[B]")

    screen.level(4)
    screen.move(left_x + 18, y - 2)
    screen.line(right_x - 18, y - 2)
    screen.stroke()
    screen.move(left_x + 18, y + 2)
    screen.line(right_x - 18, y + 2)
    screen.stroke()

    screen.level(15)
    screen.move(marker_x, y - 8)
    screen.line(marker_x, y + 8)
    screen.stroke()
end

local function pct(v)
    return string.format("%d%%", math.floor(v * 100 + 0.5))
end

local function draw_perform_page()
    draw_header(page_labels.perform)
    draw_crossfader(24)

    screen.level(8)
    screen.move(6, 40); screen.text(effect_labels[state.scenes.A.effect])
    screen.move(122, 40); screen.text_right(effect_labels[state.scenes.B.effect])

    screen.level(15)
    screen.move(6, 52); screen.text(pct(state.scenes.A.amount))
    screen.move(122, 52); screen.text_right(pct(state.scenes.B.amount))

    screen.level(6)
    screen.move(6, 62)
    screen.text(string.format("E1/K2/K3 page  E3 xfade %s", pct(state.xfade)))
end

local function draw_scene_page(scene_name)
    local scene = state.scenes[scene_name]
    draw_header(page_labels[current_page()])

    screen.level(state.cursor == 1 and 15 or 4)
    screen.move(6, 24); screen.text("effect")
    screen.move(122, 24); screen.text_right(effect_labels[scene.effect])

    screen.level(state.cursor == 2 and 15 or 4)
    screen.move(6, 38); screen.text("amount")
    screen.move(122, 38); screen.text_right(pct(scene.amount))

    screen.level(5)
    screen.move(6, 54)
    screen.text("K1 jump  E2 sel  E3 edit")

    draw_crossfader(61)
end

-- ---------------------------------------------------------------------------
-- Norns lifecycle
-- ---------------------------------------------------------------------------

function init()
    -- The engine's alloc (SynthDef compilation + Synth instantiation) runs
    -- asynchronously. Sending engine commands immediately in init() races
    -- against alloc completing and will hit nil synth references in SC.
    -- A short clock.sleep gives alloc time to finish before we push state.
    clock.run(function()
        clock.sleep(0.5)
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
            state.cursor = clamp(state.cursor + d, 1, 2)
        end
    elseif n == 3 then
        if page == "perform" then
            state.xfade = clamp(state.xfade + d * 0.02, 0, 1)
            apply_bundle()
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
            state.xfade = 0.5
        elseif page == "scene_a" then
            state.xfade = 0.0
        elseif page == "scene_b" then
            state.xfade = 1.0
        end
        apply_bundle()
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
