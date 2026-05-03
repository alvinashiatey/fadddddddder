-- fadddddddder: octo-inspired scene crossfader for live input
-- v0.1.0 @alvinashiatey

local pages = { "perform", "scene_a", "scene_b" }
local page_labels = {
    perform = "perform",
    scene_a = "scene A",
    scene_b = "scene B",
}

local effect_order = { "thru", "lp", "hp", "dub", "micro", "freeze" }
local effect_labels = {
    thru = "thru",
    lp = "lowpass",
    hp = "highpass",
    dub = "dub echo",
    micro = "microloop",
    freeze = "freeze",
}

local BUFFER_START = 1.0
local redraw_metro

local state = {
    page_index = 1,
    cursor = 1,
    xfade = 0.0,
    dry = 0.9,
    wet = 1.0,
    input_to_cut = 1.0,
    slew = 0.12,
    base_loop = 1.0,
    scenes = {
        A = { effect = "thru", amount = 0.0 },
        B = { effect = "dub", amount = 0.55 },
    },
}

local current_bundle = nil

local function clamp(value, min_value, max_value)
    return util.clamp(value, min_value, max_value)
end

local function current_page()
    return pages[state.page_index]
end

local function scene_for_page(page)
    if page == "scene_a" then
        return "A"
    elseif page == "scene_b" then
        return "B"
    end
    return nil
end

local function effect_index(name)
    for i, effect_name in ipairs(effect_order) do
        if effect_name == name then
            return i
        end
    end
    return 1
end

local function blend(a, b, x)
    return a + (b - a) * x
end

local function build_bundle(scene)
    local amount = clamp(scene.amount, 0, 1)
    local loop_len = state.base_loop

    local bundle = {
        wet = state.wet,
        dry = 1.0,
        rec = 1.0,
        pre = 1.0,
        rate = 1.0,
        loop_len = loop_len,
        fade = 0.03,
        cutoff = 12000,
        rq = 2.2,
        lp_mix = 0.0,
        hp_mix = 0.0,
        dry_mix = 1.0,
    }

    if scene.effect == "thru" then
        bundle.wet = state.wet * (0.45 + amount * 0.2)
        bundle.dry = 1.0
        bundle.rec = 1.0
        bundle.pre = 1.0
    elseif scene.effect == "lp" then
        bundle.cutoff = util.linexp(0, 1, 12000, 250, amount)
        bundle.rq = blend(2.4, 0.8, amount)
        bundle.lp_mix = 1.0
        bundle.hp_mix = 0.0
        bundle.dry_mix = blend(1.0, 0.15, amount)
        bundle.wet = state.wet * 0.95
    elseif scene.effect == "hp" then
        bundle.cutoff = util.linexp(0, 1, 40, 5000, amount)
        bundle.rq = blend(2.4, 1.0, amount)
        bundle.lp_mix = 0.0
        bundle.hp_mix = 1.0
        bundle.dry_mix = blend(1.0, 0.1, amount)
        bundle.wet = state.wet * 0.95
    elseif scene.effect == "dub" then
        bundle.rec = blend(0.75, 0.15, amount)
        bundle.pre = blend(1.0, 0.82, amount)
        bundle.loop_len = blend(loop_len, math.max(0.2, loop_len * 3.0), amount)
        bundle.fade = blend(0.03, 0.08, amount)
        bundle.cutoff = util.linexp(0, 1, 9000, 900, amount)
        bundle.lp_mix = blend(0.0, 0.8, amount)
        bundle.dry_mix = 1.0
        bundle.wet = state.wet * blend(0.7, 1.1, amount)
        bundle.dry = blend(1.0, 0.75, amount)
    elseif scene.effect == "micro" then
        bundle.rec = blend(0.65, 0.0, amount)
        bundle.pre = 1.0
        bundle.rate = blend(1.0, 0.5, amount)
        bundle.loop_len = util.linexp(0, 1, loop_len, 0.08, amount)
        bundle.fade = blend(0.025, 0.008, amount)
        bundle.cutoff = util.linexp(0, 1, 10000, 1400, amount)
        bundle.lp_mix = blend(0.0, 0.6, amount)
        bundle.dry_mix = blend(1.0, 0.0, amount)
        bundle.wet = state.wet * 1.1
        bundle.dry = blend(0.95, 0.35, amount)
    elseif scene.effect == "freeze" then
        bundle.rec = 0.0
        bundle.pre = 1.0
        bundle.rate = blend(1.0, 0.82, amount)
        bundle.loop_len = util.linexp(0, 1, loop_len, 0.12, amount)
        bundle.fade = blend(0.03, 0.01, amount)
        bundle.cutoff = util.linexp(0, 1, 11000, 650, amount)
        bundle.lp_mix = blend(0.0, 0.9, amount)
        bundle.dry_mix = blend(1.0, 0.0, amount)
        bundle.wet = state.wet
        bundle.dry = blend(0.9, 0.2, amount)
    end

    bundle.wet = clamp(bundle.wet, 0, 1.2)
    bundle.dry = clamp(bundle.dry, 0, 1.0)
    bundle.rec = clamp(bundle.rec, 0, 1.0)
    bundle.pre = clamp(bundle.pre, 0, 1.0)
    bundle.loop_len = clamp(bundle.loop_len, 0.08, 8.0)
    bundle.fade = clamp(bundle.fade, 0.005, 0.2)
    bundle.cutoff = clamp(bundle.cutoff, 20, 12000)
    bundle.rq = clamp(bundle.rq, 0.4, 4.0)
    bundle.lp_mix = clamp(bundle.lp_mix, 0, 1.0)
    bundle.hp_mix = clamp(bundle.hp_mix, 0, 1.0)
    bundle.dry_mix = clamp(bundle.dry_mix, 0, 1.0)

    return bundle
end

local function interpolated_bundle()
    local left = build_bundle(state.scenes.A)
    local right = build_bundle(state.scenes.B)
    local x = state.xfade

    return {
        wet = blend(left.wet, right.wet, x),
        dry = blend(left.dry, right.dry, x),
        rec = blend(left.rec, right.rec, x),
        pre = blend(left.pre, right.pre, x),
        rate = blend(left.rate, right.rate, x),
        loop_len = blend(left.loop_len, right.loop_len, x),
        fade = blend(left.fade, right.fade, x),
        cutoff = blend(left.cutoff, right.cutoff, x),
        rq = blend(left.rq, right.rq, x),
        lp_mix = blend(left.lp_mix, right.lp_mix, x),
        hp_mix = blend(left.hp_mix, right.hp_mix, x),
        dry_mix = blend(left.dry_mix, right.dry_mix, x),
    }
end

local function apply_bundle()
    current_bundle = interpolated_bundle()

    audio.level_monitor(state.dry * current_bundle.dry)
    audio.level_adc_cut(state.input_to_cut)

    for voice = 1, 2 do
        softcut.level(voice, current_bundle.wet)
        softcut.rate(voice, current_bundle.rate)
        softcut.rec_level(voice, current_bundle.rec)
        softcut.pre_level(voice, current_bundle.pre)
        softcut.fade_time(voice, current_bundle.fade)
        softcut.loop_start(voice, BUFFER_START)
        softcut.loop_end(voice, BUFFER_START + current_bundle.loop_len)
        softcut.post_filter_fc(voice, current_bundle.cutoff)
        softcut.post_filter_rq(voice, current_bundle.rq)
        softcut.post_filter_lp(voice, current_bundle.lp_mix)
        softcut.post_filter_hp(voice, current_bundle.hp_mix)
        softcut.post_filter_dry(voice, current_bundle.dry_mix)
        softcut.post_filter_bp(voice, 0.0)
        softcut.post_filter_br(voice, 0.0)
    end
end

local function reframe_positions()
    for voice = 1, 2 do
        softcut.position(voice, BUFFER_START)
    end
end

local function setup_softcut()
    audio.level_adc(1.0)
    audio.level_dac(1.0)
    audio.level_monitor(0.0)
    audio.level_cut(1.0)
    audio.level_adc_cut(state.input_to_cut)
    audio.level_eng_cut(0.0)
    audio.level_tape_cut(0.0)

    softcut.reset()
    softcut.buffer_clear()

    for voice = 1, 2 do
        softcut.enable(voice, 1)
        softcut.buffer(voice, voice)
        softcut.play(voice, 1)
        softcut.rec(voice, 1)
        softcut.loop(voice, 1)
        softcut.loop_start(voice, BUFFER_START)
        softcut.loop_end(voice, BUFFER_START + state.base_loop)
        softcut.position(voice, BUFFER_START)
        softcut.rate(voice, 1.0)
        softcut.level(voice, 1.0)
        softcut.fade_time(voice, 0.03)
        softcut.rec_level(voice, 1.0)
        softcut.pre_level(voice, 1.0)
        softcut.level_slew_time(voice, state.slew)
        softcut.rate_slew_time(voice, state.slew)
        softcut.recpre_slew_time(voice, state.slew)
        softcut.post_filter_fc(voice, 12000)
        softcut.post_filter_rq(voice, 2.0)
        softcut.post_filter_dry(voice, 1.0)
        softcut.post_filter_lp(voice, 0.0)
        softcut.post_filter_hp(voice, 0.0)
        softcut.post_filter_bp(voice, 0.0)
        softcut.post_filter_br(voice, 0.0)
    end

    softcut.pan(1, -1.0)
    softcut.pan(2, 1.0)

    softcut.level_input_cut(1, 1, 1.0)
    softcut.level_input_cut(2, 1, 0.0)
    softcut.level_input_cut(1, 2, 0.0)
    softcut.level_input_cut(2, 2, 1.0)

    softcut.level_cut_cut(1, 1, 0.0)
    softcut.level_cut_cut(2, 2, 0.0)
    softcut.level_cut_cut(1, 2, 0.0)
    softcut.level_cut_cut(2, 1, 0.0)

    apply_bundle()
end

local function change_page(delta)
    state.page_index = ((state.page_index - 1 + delta) % #pages) + 1
    state.cursor = 1
end

local function adjust_scene(scene_name, delta)
    local scene = state.scenes[scene_name]

    if state.cursor == 1 then
        local index = effect_index(scene.effect)
        index = clamp(index + delta, 1, #effect_order)
        scene.effect = effect_order[index]
    elseif state.cursor == 2 then
        scene.amount = clamp(scene.amount + delta * 0.02, 0, 1)
    end

    apply_bundle()
end

local function draw_header(title)
    screen.level(5)
    screen.move(4, 8)
    screen.text("fadddddddder")
    screen.move(124, 8)
    screen.text_right(title)
end

local function draw_crossfader(y)
    local left_x = 16
    local right_x = 112
    local center_y = y
    local marker_x = blend(left_x, right_x, state.xfade)

    screen.level(15)
    screen.move(4, center_y)
    screen.text("[A]")
    screen.move(124, center_y)
    screen.text_right("[B]")

    screen.level(4)
    screen.move(left_x + 18, center_y - 2)
    screen.line(right_x - 18, center_y - 2)
    screen.stroke()

    screen.move(left_x + 18, center_y + 2)
    screen.line(right_x - 18, center_y + 2)
    screen.stroke()

    screen.level(15)
    screen.move(marker_x, center_y - 8)
    screen.line(marker_x, center_y + 8)
    screen.stroke()
end

local function draw_perform_page()
    draw_header(page_labels.perform)
    draw_crossfader(24)

    screen.level(8)
    screen.move(6, 40)
    screen.text(effect_labels[state.scenes.A.effect])
    screen.move(122, 40)
    screen.text_right(effect_labels[state.scenes.B.effect])

    screen.level(15)
    screen.move(6, 52)
    screen.text(string.format("%d%%", math.floor(state.scenes.A.amount * 100 + 0.5)))
    screen.move(122, 52)
    screen.text_right(string.format("%d%%", math.floor(state.scenes.B.amount * 100 + 0.5)))

    screen.level(6)
    screen.move(48, 62)
    screen.text(string.format("xfade %d%%", math.floor(state.xfade * 100 + 0.5)))
end

local function draw_scene_page(scene_name)
    local scene = state.scenes[scene_name]
    draw_header(page_labels[current_page()])

    screen.level(state.cursor == 1 and 15 or 4)
    screen.move(6, 24)
    screen.text("effect")
    screen.move(122, 24)
    screen.text_right(effect_labels[scene.effect])

    screen.level(state.cursor == 2 and 15 or 4)
    screen.move(6, 38)
    screen.text("amount")
    screen.move(122, 38)
    screen.text_right(string.format("%d%%", math.floor(scene.amount * 100 + 0.5)))

    screen.level(5)
    screen.move(6, 54)
    screen.text("K2/K3 page  E2 sel  E3 edit")

    draw_crossfader(61)
end

function init()
    setup_softcut()

    redraw_metro = metro.init()
    redraw_metro.time = 1 / 15
    redraw_metro.event = function()
        redraw()
    end
    redraw_metro:start()
end

function enc(n, d)
    local page = current_page()

    if n == 1 then
        change_page(d > 0 and 1 or -1)
    elseif n == 2 then
        if page ~= "perform" then
            state.cursor = clamp(state.cursor + (d > 0 and 1 or -1), 1, 2)
        end
    elseif n == 3 then
        if page == "perform" then
            state.xfade = clamp(state.xfade + d * 0.02, 0, 1)
            apply_bundle()
        else
            local scene_name = scene_for_page(page)
            adjust_scene(scene_name, d > 0 and 1 or -1)
        end
    end

    redraw()
end

function key(n, z)
    if z == 0 then
        return
    end

    local page = current_page()

    if n == 2 then
        if page == "perform" then
            state.xfade = 0.0
            apply_bundle()
        else
            change_page(-1)
        end
    elseif n == 3 then
        if page == "perform" then
            state.xfade = 1.0
            apply_bundle()
        else
            change_page(1)
        end
    elseif n == 1 then
        if page == "perform" then
            state.xfade = 0.5
            apply_bundle()
        else
            reframe_positions()
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
    end

    audio.level_monitor(0.0)
    audio.level_adc_cut(0.0)
    softcut.poll_stop_phase()
end
