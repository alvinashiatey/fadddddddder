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

	local NUM_SCENES = 16
	local data_dir = _path.data .. "fadddddddder/"
	local bank_file = data_dir .. "scene_bank.data"

	local state = {
		page_index = 1,
		cursor = 1,
		xfade = 0.0,
		slots = { A = 1, B = 1 },
		bank = { A = {}, B = {} },
	}

	local redraw_metro = nil
	local engine_ready = false
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
		return Store.ensure_bank(state)
	end

	local scene_for_side = function(side)
		return SceneModel.scene_for_side(state, side, NUM_SCENES, ensure_bank)
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
		return EngineAdapter.apply_bundle(state, engine_ready)
	end

	local function apply_global_engine_params()
		return EngineAdapter.apply_global_params(engine_ready)
	end

	local Pages = include("lib/fadddddddder/pages")({
		state = state,
		clamp = clamp,
		linlin = linlin,
		num_scenes = NUM_SCENES,
		effect_order = Effects.order,
		effect_labels = Effects.labels,
		effect_params = Effects.params,
		effect_param_counts = Effects.param_counts,
		default_values = Effects.default_values,
		effect_index_map = Effects.index_map,
		linked_filter_modes = Effects.linked_filter_modes,
		scene_for_side = scene_for_side,
		values_for_scene = values_for_scene,
		cursor_max_for_scene = cursor_max_for_scene,
		apply_bundle = apply_bundle,
		request_save = Store.request_save,
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

		Store.load_bank(state)

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
			if Store.save_pending then
				Store.save_bank(state)
			end
			redraw()
		end
		redraw_metro:start()
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
		Store.save_bank(state)
		if redraw_metro then
			redraw_metro:stop()
			redraw_metro = nil
		end
	end

	return App
end
