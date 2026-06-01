return function(deps)
	local state = deps.state
	local clamp = deps.clamp
	local linlin = deps.linlin
	local num_scenes = deps.num_scenes
	local effect_order = deps.effect_order
	local effect_labels = deps.effect_labels
	local effect_params = deps.effect_params
	local effect_param_counts = deps.effect_param_counts
	local default_values = deps.default_values
	local effect_index_map = deps.effect_index_map
	local linked_filter_modes = deps.linked_filter_modes
	local scene_for_side = deps.scene_for_side
	local values_for_scene = deps.values_for_scene
	local cursor_max_for_scene = deps.cursor_max_for_scene
	local apply_bundle = deps.apply_bundle
	local request_save = deps.request_save

	local Pages = {}
	local page_order = { "perform", "scene_a", "scene_b" }
	local page_labels = { perform = "perform", scene_a = "scene A", scene_b = "scene B" }
	local interaction = {
		k1_down = false,
		k1_used_as_modifier = false,
	}

	local function pct(v)
		return string.format("%d%%", math.floor(v * 100 + 0.5))
	end

	local function short_pct(v)
		return string.format("%02d", math.floor(v * 99 + 0.5))
	end

	local function slot_label(side)
		return string.format("%s%02d", side, state.slots[side])
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

	local function set_slot(side, slot)
		state.slots[side] = clamp(slot, 1, num_scenes)
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

			if linked_filter_modes[prev_effect] and linked_filter_modes[scene.effect] then
				local next_values = scene.values[scene.effect]
				next_values.amount = values.amount
				next_values.param1 = values.param1
				next_values.param2 = values.param2
				next_values.param4 = values.param4
				next_values.param3 = default_values[scene.effect].param3
			end

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

	function Pages.current_page()
		return page_order[state.page_index]
	end

	function Pages.change_page(delta)
		state.page_index = clamp(state.page_index + delta, 1, #page_order)
		state.cursor = 1
	end

	function Pages.side_for_page(page)
		if page == "scene_a" then
			return "A"
		elseif page == "scene_b" then
			return "B"
		end
	end

	function Pages.handle_enc(n, d)
		local page = Pages.current_page()

		if n == 1 then
			Pages.change_page(d > 0 and 1 or -1)
		elseif n == 2 then
			if page == "perform" then
				if interaction.k1_down then
					interaction.k1_used_as_modifier = true
					set_slot("B", state.slots.B + d)
				else
					set_slot("A", state.slots.A + d)
				end
			else
				local side = Pages.side_for_page(page)
				state.cursor = clamp(state.cursor + d, 1, cursor_max_for_scene(scene_for_side(side)))
			end
		elseif n == 3 then
			if page == "perform" then
				state.xfade = clamp(state.xfade + d * 0.02, 0, 1)
				apply_bundle()
				request_save()
			else
				local side = Pages.side_for_page(page)
				if side then
					adjust_scene(side, d)
				end
			end
		end
	end

	function Pages.handle_key(n, z)
		local page = Pages.current_page()

		if n == 1 then
			if z == 1 then
				interaction.k1_down = true
				interaction.k1_used_as_modifier = false
			else
				interaction.k1_down = false
				if not interaction.k1_used_as_modifier then
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
			Pages.change_page(-1)
		elseif z == 1 and n == 3 then
			Pages.change_page(1)
		end
	end

	function Pages.draw_perform_page()
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

	function Pages.draw_scene_page(side)
		local scene = scene_for_side(side)
		local values = values_for_scene(scene)
		local param_labels = effect_params[scene.effect]
		local title = string.format("%s %02d", page_labels[Pages.current_page()], state.slots[side])

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

	function Pages.redraw()
		screen.clear()
		local page = Pages.current_page()
		if page == "perform" then
			Pages.draw_perform_page()
		elseif page == "scene_a" then
			Pages.draw_scene_page("A")
		elseif page == "scene_b" then
			Pages.draw_scene_page("B")
		end
		screen.update()
	end

	return Pages
end
