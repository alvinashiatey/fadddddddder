return function(Effects)
	local effect_order = Effects.order
	local effect_param_counts = Effects.param_counts
	local default_values = Effects.default_values
	local effect_index_map = Effects.index_map
	local legacy_effect_map = Effects.legacy_effect_map

	local clamp = util.clamp

	local SceneModel = {}

	function SceneModel.clone_values(v)
		return { amount = v.amount, param1 = v.param1, param2 = v.param2, param3 = v.param3, param4 = v.param4 }
	end

	function SceneModel.default_effect_values()
		local values = {}
		for _, id in ipairs(effect_order) do
			values[id] = SceneModel.clone_values(default_values[id])
		end
		return values
	end

	function SceneModel.new_scene_slot(index)
		return {
			effect = index == 1 and "dub_bloom" or "thru",
			values = SceneModel.default_effect_values(),
		}
	end

	function SceneModel.sanitize_slot(slot, index)
		-- Reject anything that isn't a plain table (nil, strings, numbers, etc).
		if type(slot) ~= "table" then
			return SceneModel.new_scene_slot(index)
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
			slot.values = SceneModel.default_effect_values()
			return slot
		end

		local loop_ok = pcall(function()
			for _, id in ipairs(effect_order) do
				local defaults = default_values[id]
				local v = slot.values[id]
				if type(v) ~= "table" then
					slot.values[id] = SceneModel.clone_values(defaults)
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
			slot.values = SceneModel.default_effect_values()
		end
		return slot
	end

	function SceneModel.clone_slot(slot, index)
		-- sanitize_slot returns a fresh slot for nil/non-table inputs.
		slot = SceneModel.sanitize_slot(slot, index)
		local out = { effect = slot.effect, values = {} }
		for _, id in ipairs(effect_order) do
			out.values[id] = SceneModel.clone_values(slot.values[id])
		end
		return out
	end

	function SceneModel.scene_for_side(state, side, num_scenes, ensure_bank)
		if type(state.bank) ~= "table" or type(state.bank[side]) ~= "table" then
			ensure_bank()
		end
		local slot_index = clamp(tonumber(state.slots[side]) or 1, 1, num_scenes)
		local scene = state.bank[side][slot_index]
		if type(scene) ~= "table" then
			scene = SceneModel.sanitize_slot(scene, slot_index)
			state.bank[side][slot_index] = scene
		end
		return scene
	end

	function SceneModel.values_for_scene(scene)
		if type(scene) ~= "table" then
			scene = SceneModel.new_scene_slot(1)
		end
		if type(scene.values) ~= "table" then
			scene.values = SceneModel.default_effect_values()
		end
		if type(scene.values[scene.effect]) ~= "table" then
			scene.values[scene.effect] = SceneModel.clone_values(default_values[scene.effect] or default_values.thru)
		end
		return scene.values[scene.effect]
	end

	function SceneModel.cursor_max_for_scene(scene)
		return 2 + (effect_param_counts[scene.effect] or 4)
	end

	return SceneModel
end
