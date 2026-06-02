local tabutil = require("tabutil")

return function(deps)
	local engine = deps.engine
	local params = deps.params
	local audio = deps.audio
	local clock = deps.clock
	local metro = deps.metro
	local redraw = deps.redraw
	local util = deps.util

	local Effects = include("lib/fadddddddder/effects")
	local SceneModel = include("lib/fadddddddder/scene_model")(Effects)
	local Params = include("lib/fadddddddder/params")
	local State = include("lib/fadddddddder/state")

	local NUM_SCENES = 16
	local data_dir = _path.data .. "fadddddddder/"
	local bank_file = data_dir .. "scene_bank.data"

	local state = State.new()
	local persisted = state.persisted
	local ui = state.ui
	local interaction = state.interaction
	local runtime = state.runtime
	local clamp = util.clamp
	local linlin = util.linlin

	local Store = include("lib/fadddddddder/store")({
		tabutil = tabutil,
		clamp = clamp,
		num_scenes = NUM_SCENES,
		data_dir = data_dir,
		bank_file = bank_file,
		clone_slot = SceneModel.clone_slot,
		sanitize_slot = SceneModel.sanitize_slot,
	})

	local ensure_bank = function()
		return Store.ensure_bank(persisted)
	end

	local scene_for_side = function(side)
		return SceneModel.scene_for_side(persisted, side, NUM_SCENES, ensure_bank)
	end

	local values_for_scene = SceneModel.values_for_scene
	local cursor_max_for_scene = SceneModel.cursor_max_for_scene

	local EngineAdapter = include("lib/fadddddddder/engine_adapter")({
		engine = engine,
		params = params,
		engine_effect_index = Effects.engine_effect_index,
		effect_engine_map = Effects.engine_map,
		scene_for_side = scene_for_side,
		values_for_scene = values_for_scene,
	})

	local function apply_bundle()
		return EngineAdapter.apply_bundle(persisted, runtime.engine_ready)
	end

	local function apply_global_engine_params()
		return EngineAdapter.apply_global_params(runtime.engine_ready)
	end

	local function slot_label(side)
		return string.format("%s%02d", side, persisted.slots[side])
	end

	local function change_page(delta)
		ui.page_index = clamp(ui.page_index + delta, 1, 3)
		ui.cursor = 1
	end

	local function current_page()
		local pages = { "perform", "scene_a", "scene_b" }
		return pages[ui.page_index]
	end

	local function scene_side_for_page(page)
		if page == "scene_a" then
			return "A"
		elseif page == "scene_b" then
			return "B"
		end
	end

	local function select_slot_delta(side, delta)
		persisted.slots[side] = clamp(persisted.slots[side] + delta, 1, NUM_SCENES)
		ui.cursor = 1
		apply_bundle()
		Store.request_save()
	end

	local function move_cursor_delta(delta, side)
		ui.cursor = clamp(ui.cursor + delta, 1, cursor_max_for_scene(scene_for_side(side)))
	end

	local function adjust_xfade_delta(delta)
		persisted.xfade = clamp(persisted.xfade + delta * 0.02, 0, 1)
		apply_bundle()
		Store.request_save()
	end

	local function adjust_scene_delta(side, delta)
		local scene = scene_for_side(side)
		local values = values_for_scene(scene)

		if ui.cursor == 1 then
			local prev_effect = scene.effect
			local idx = clamp(Effects.index_map[scene.effect] + delta, 1, #Effects.order)
			scene.effect = Effects.order[idx]

			if Effects.linked_filter_modes[prev_effect] and Effects.linked_filter_modes[scene.effect] then
				local next_values = scene.values[scene.effect]
				next_values.amount = values.amount
				next_values.param1 = values.param1
				next_values.param2 = values.param2
				next_values.param4 = values.param4
				next_values.param3 = Effects.default_values[scene.effect].param3
			end

			ui.cursor = clamp(ui.cursor, 1, cursor_max_for_scene(scene))
		elseif ui.cursor == 2 then
			values.amount = clamp(values.amount + delta * 0.02, 0, 1)
		elseif ui.cursor == 3 then
			values.param1 = clamp(values.param1 + delta * 0.02, 0, 1)
		elseif ui.cursor == 4 then
			values.param2 = clamp(values.param2 + delta * 0.02, 0, 1)
		elseif ui.cursor == 5 then
			values.param3 = clamp(values.param3 + delta * 0.02, 0, 1)
		else
			values.param4 = clamp(values.param4 + delta * 0.02, 0, 1)
		end

		apply_bundle()
		Store.request_save()
	end

	local function snap_xfade_for_page(page)
		if page == "perform" then
			persisted.xfade = 0.5
		elseif page == "scene_a" then
			persisted.xfade = 0.0
		elseif page == "scene_b" then
			persisted.xfade = 1.0
		end
		apply_bundle()
		Store.request_save()
	end

	local function get_perform_view()
		local scene_a = scene_for_side("A")
		local scene_b = scene_for_side("B")
		local values_a = values_for_scene(scene_a)
		local values_b = values_for_scene(scene_b)
		return {
			title = "perform",
			xfade = persisted.xfade,
			slot_labels = { A = slot_label("A"), B = slot_label("B") },
			effects = { A = Effects.labels[scene_a.effect], B = Effects.labels[scene_b.effect] },
			amounts = { A = values_a.amount, B = values_b.amount },
		}
	end

	local function get_scene_view(side)
		local page_labels = { scene_a = "scene A", scene_b = "scene B" }
		local scene = scene_for_side(side)
		local values = values_for_scene(scene)
		return {
			title = string.format("%s %02d", page_labels[current_page()], persisted.slots[side]),
			effect_label = Effects.labels[scene.effect],
			amount = values.amount,
			param_labels = Effects.params[scene.effect],
			param_count = Effects.param_counts[scene.effect] or 4,
			param_values = {
				param1 = values.param1,
				param2 = values.param2,
				param3 = values.param3,
				param4 = values.param4,
			},
		}
	end

	local Pages = include("lib/fadddddddder/pages")({
		ui = ui,
		interaction = interaction,
		linlin = linlin,
		actions = {
			current_page = current_page,
			change_page = change_page,
			scene_side_for_page = scene_side_for_page,
			select_slot_delta = select_slot_delta,
			move_cursor_delta = move_cursor_delta,
			adjust_xfade_delta = adjust_xfade_delta,
			adjust_scene_delta = adjust_scene_delta,
			snap_xfade_for_page = snap_xfade_for_page,
			get_perform_view = get_perform_view,
			get_scene_view = get_scene_view,
		},
	})

	local App = {}

	function App.init()
		audio.level_adc(1.0)
		audio.level_dac(1.0)
		audio.level_monitor(1.0)

		Params({
			params = params,
			on_change = function()
				apply_global_engine_params()
			end,
		}):register()

		Store.load_bank(persisted)

		clock.run(function()
			clock.sleep(0.5)
			runtime.engine_ready = true
			engine.set_num_input_channels(2)
			engine.set_input_amp(1.0)
			engine.set_output_amp(1.0)
			apply_global_engine_params()
			apply_bundle()
		end)

		runtime.redraw_metro = metro.init()
		runtime.redraw_metro.time = 1 / 15
		runtime.redraw_metro.event = function()
			if Store.save_pending then
				Store.save_bank(persisted)
			end
			redraw()
		end
		runtime.redraw_metro:start()
	end

	function App.enc(n, d)
		Pages.handle_enc(n, d)
		redraw()
	end

	function App.key(n, z)
		Pages.handle_key(n, z)
		redraw()
	end

	function App.redraw()
		Pages.redraw()
	end

	function App.cleanup()
		Store.save_bank(persisted)
		if runtime.redraw_metro then
			runtime.redraw_metro:stop()
			runtime.redraw_metro = nil
		end
	end

	return App
end
