-- fadddddddder: octo-inspired scene crossfader for live input
-- v0.2.1 @alvinashiatey

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

-- Reverse lookup: effect name → index (built once, O(1) thereafter)
local effect_index_map = {}
for i, name in ipairs(effect_order) do effect_index_map[name] = i end

local BUFFER_START = 1.0
local DEFAULT_PLAY_LAG = 0.35
local redraw_metro = nil
local current_play_lag = DEFAULT_PLAY_LAG
local reframe_positions

local state        = {
    page_index   = 1,
    cursor       = 1,
    xfade        = 0.0,
    dry          = 1.0,
    wet          = 1.0,
    input_to_cut = 1.0,
    slew         = 0.12,
    base_loop    = 8.0,
    scenes       = {
        A = { effect = "thru", amount = 0.0 },
        B = { effect = "dub", amount = 0.55 },
    },
}

-- util.linlin replaces the local blend helper (already in norns stdlib)
local linlin       = util.linlin
local linexp       = util.linexp
local clamp        = util.clamp

-- ---------------------------------------------------------------------------
-- Bundle construction
-- ---------------------------------------------------------------------------

local function build_bundle(scene)
    local a   = clamp(scene.amount, 0, 1)
    local ll  = state.base_loop
    local wet = state.wet

    local b   = {
        wet      = 0.0,
        dry      = 1.0,
        rec      = 1.0,
        pre      = 1.0,
        rate     = 1.0,
        play_lag = DEFAULT_PLAY_LAG,
        loop_len = ll,
        fade     = 0.03,
        cutoff   = 12000,
        rq       = 2.2,
        lp_mix   = 0.0,
        hp_mix   = 0.0,
        dry_mix  = 1.0,
    }

    local eff = scene.effect
    if eff == "thru" then
        b.wet = wet * linlin(0, 1, 0.0, 0.18, a)
        b.play_lag = linlin(0, 1, 0.01, 0.03, a)
    elseif eff == "lp" then
        b.cutoff  = linexp(0, 1, 12000, 250, a)
        b.rq      = linlin(0, 1, 2.4, 0.8, a)
        b.lp_mix  = 1.0
        b.dry_mix = linlin(0, 1, 1.0, 0.0, a)
        b.wet     = wet * linlin(0, 1, 0.0, 1.0, a)
        b.dry     = linlin(0, 1, 1.0, 0.2, a)
        b.play_lag = linlin(0, 1, 0.01, 0.025, a)
    elseif eff == "hp" then
        b.cutoff  = linexp(0, 1, 40, 5000, a)
        b.rq      = linlin(0, 1, 2.4, 1.0, a)
        b.hp_mix  = 1.0
        b.dry_mix = linlin(0, 1, 1.0, 0.0, a)
        b.wet     = wet * linlin(0, 1, 0.0, 1.0, a)
        b.dry     = linlin(0, 1, 1.0, 0.2, a)
        b.play_lag = linlin(0, 1, 0.01, 0.025, a)
    elseif eff == "dub" then
        b.rec      = linlin(0, 1, 0.65, 0.18, a)
        b.pre      = linlin(0, 1, 1.0, 0.88, a)
        b.loop_len = linlin(0, 1, ll, math.max(2.0, ll), a)
        b.fade     = linlin(0, 1, 0.03, 0.06, a)
        b.cutoff   = linexp(0, 1, 9000, 900, a)
        b.lp_mix   = linlin(0, 1, 0.0, 0.8, a)
        b.dry_mix  = 1.0
        b.wet      = wet * linlin(0, 1, 0.2, 1.0, a)
        b.dry      = linlin(0, 1, 1.0, 0.55, a)
        b.play_lag = linlin(0, 1, 0.08, 0.35, a)
    elseif eff == "micro" then
        b.rec      = linlin(0, 1, 0.35, 0.0, a)
        b.rate     = linlin(0, 1, 1.0, 0.65, a)
        b.loop_len = linexp(0, 1, ll, 0.12, a)
        b.fade     = linlin(0, 1, 0.025, 0.01, a)
        b.cutoff   = linexp(0, 1, 10000, 1400, a)
        b.lp_mix   = linlin(0, 1, 0.0, 0.6, a)
        b.dry_mix  = linlin(0, 1, 1.0, 0.0, a)
        b.wet      = wet * linlin(0, 1, 0.25, 1.0, a)
        b.dry      = linlin(0, 1, 1.0, 0.25, a)
        b.play_lag = linlin(0, 1, 0.06, 0.2, a)
    elseif eff == "freeze" then
        b.rec      = 0.0
        b.rate     = linlin(0, 1, 1.0, 0.82, a)
        b.loop_len = linexp(0, 1, ll, 0.2, a)
        b.fade     = linlin(0, 1, 0.03, 0.01, a)
        b.cutoff   = linexp(0, 1, 11000, 650, a)
        b.lp_mix   = linlin(0, 1, 0.0, 0.9, a)
        b.dry_mix  = linlin(0, 1, 1.0, 0.0, a)
        b.wet      = wet * linlin(0, 1, 0.35, 1.0, a)
        b.dry      = linlin(0, 1, 1.0, 0.15, a)
        b.play_lag = linlin(0, 1, 0.08, 0.22, a)
    end

    b.wet      = clamp(b.wet, 0, 1.2)
    b.dry      = clamp(b.dry, 0, 1.0)
    b.rec      = clamp(b.rec, 0, 1.0)
    b.pre      = clamp(b.pre, 0, 1.0)
    b.play_lag = clamp(b.play_lag, 0.005, 0.5)
    b.loop_len = clamp(b.loop_len, 0.08, 8.0)
    b.fade     = clamp(b.fade, 0.005, 0.2)
    b.cutoff   = clamp(b.cutoff, 20, 12000)
    b.rq       = clamp(b.rq, 0.4, 4.0)
    b.lp_mix   = clamp(b.lp_mix, 0, 1.0)
    b.hp_mix   = clamp(b.hp_mix, 0, 1.0)
    b.dry_mix  = clamp(b.dry_mix, 0, 1.0)
    return b
end

local function apply_bundle()
    local L = build_bundle(state.scenes.A)
    local R = build_bundle(state.scenes.B)
    local x = state.xfade

    -- Inline interpolation avoids a third table allocation
    audio.level_monitor(state.dry * linlin(0, 1, L.dry, R.dry, x))
    audio.level_adc_cut(state.input_to_cut)

    local wet      = linlin(0, 1, L.wet, R.wet, x)
    local rate     = linlin(0, 1, L.rate, R.rate, x)
    local rec      = linlin(0, 1, L.rec, R.rec, x)
    local pre      = linlin(0, 1, L.pre, R.pre, x)
    local play_lag = linlin(0, 1, L.play_lag, R.play_lag, x)
    local fade     = linlin(0, 1, L.fade, R.fade, x)
    local loop_len = linlin(0, 1, L.loop_len, R.loop_len, x)
    local cutoff   = linlin(0, 1, L.cutoff, R.cutoff, x)
    local rq       = linlin(0, 1, L.rq, R.rq, x)
    local lp_mix   = linlin(0, 1, L.lp_mix, R.lp_mix, x)
    local hp_mix   = linlin(0, 1, L.hp_mix, R.hp_mix, x)
    local dry_mix  = linlin(0, 1, L.dry_mix, R.dry_mix, x)

    for voice = 1, 2 do
        softcut.level(voice, wet)
        softcut.rate(voice, rate)
        softcut.rec_level(voice, rec)
        softcut.pre_level(voice, pre)
        softcut.rec_offset(voice, play_lag)
        softcut.fade_time(voice, fade)
        softcut.loop_start(voice, BUFFER_START)
        softcut.loop_end(voice, BUFFER_START + loop_len)
        softcut.post_filter_fc(voice, cutoff)
        softcut.post_filter_rq(voice, rq)
        softcut.post_filter_lp(voice, lp_mix)
        softcut.post_filter_hp(voice, hp_mix)
        softcut.post_filter_dry(voice, dry_mix)
        softcut.post_filter_bp(voice, 0.0)
        softcut.post_filter_br(voice, 0.0)
    end

    if math.abs(play_lag - current_play_lag) > 0.02 then
        current_play_lag = play_lag
        reframe_positions()
    else
        current_play_lag = play_lag
    end
end

-- ---------------------------------------------------------------------------
-- Softcut setup
-- ---------------------------------------------------------------------------

reframe_positions = function()
    for voice = 1, 2 do softcut.position(voice, BUFFER_START + current_play_lag) end
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
        softcut.position(voice, BUFFER_START + current_play_lag)
        softcut.rate(voice, 1.0)
        softcut.level(voice, 1.0)
        softcut.fade_time(voice, 0.03)
        softcut.rec_level(voice, 1.0)
        softcut.pre_level(voice, 1.0)
        softcut.rec_offset(voice, current_play_lag)
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
        -- Velocity-sensitive: fast spin jumps multiple effects
        local idx = clamp(effect_index_map[scene.effect] + d, 1, #effect_order)
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
    screen.text("E1/K2/K3 page  E2 sel  E3 edit")

    draw_crossfader(61)
end

-- ---------------------------------------------------------------------------
-- Norns lifecycle
-- ---------------------------------------------------------------------------

function init()
    setup_softcut()
    redraw_metro = metro.init()
    redraw_metro.time = 1 / 15
    redraw_metro.event = redraw
    redraw_metro:start()
    redraw()
end

function enc(n, d)
    local page = current_page()
    if n == 1 then
        -- E1 still snaps to ±1 page regardless of speed
        change_page(d > 0 and 1 or -1)
    elseif n == 2 then
        if page ~= "perform" then
            -- Velocity-sensitive cursor (clamped to 1-2)
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
        redraw_metro = nil
    end
    audio.level_monitor(0.0)
    audio.level_adc_cut(0.0)
    softcut.poll_stop_phase()
end
