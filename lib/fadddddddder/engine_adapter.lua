return function(deps)
	local engine = deps.engine
	local params = deps.params
	local engine_effect_index = deps.engine_effect_index
	local effect_engine_map = deps.effect_engine_map
	local scene_for_side = deps.scene_for_side
	local values_for_scene = deps.values_for_scene

	local EngineAdapter = {}

	function EngineAdapter.effect_engine_index_for(name)
		return engine_effect_index[effect_engine_map[name] or "thru"] or 0
	end

	function EngineAdapter.sync_scene(side)
		if not engine.set_scene_a_effect then
			return
		end
		local scene = scene_for_side(side)
		local values = values_for_scene(scene)
		local idx = EngineAdapter.effect_engine_index_for(scene.effect)

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

	function EngineAdapter.apply_bundle(state, engine_ready)
		if not engine_ready then
			return
		end
		EngineAdapter.sync_scene("A")
		EngineAdapter.sync_scene("B")
		engine.set_xfade(state.xfade)
	end

	function EngineAdapter.apply_global_params(engine_ready)
		if not engine_ready then
			return
		end
		engine.set_bpm(params:get("delay_bpm"))
		engine.set_delay_sync(params:get("delay_sync") == 2 and 1 or 0)
	end

	return EngineAdapter
end
