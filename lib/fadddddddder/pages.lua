return function(deps)
	local ui = deps.ui
	local interaction = deps.interaction
	local linlin = deps.linlin
	local actions = deps.actions

	local Pages = {}

	local function pct(v)
		return string.format("%d%%", math.floor(v * 100 + 0.5))
	end

	local function short_pct(v)
		return string.format("%02d", math.floor(v * 99 + 0.5))
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

	local function draw_crossfader(y, view)
		local left_x = 20
		local right_x = 108
		local marker_x = linlin(0, 1, left_x, right_x, view.xfade)

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
		screen.text(view.slot_labels.A)
		screen.move(right_x, y + 16)
		screen.text_right(view.slot_labels.B)
	end

	function Pages.handle_enc(n, d)
		local page = actions.current_page()

		if n == 1 then
			actions.change_page(d > 0 and 1 or -1)
		elseif n == 2 then
			if page == "perform" then
				if interaction.k1_down then
					interaction.k1_used_as_modifier = true
					actions.select_slot_delta("B", d)
				else
					actions.select_slot_delta("A", d)
				end
			else
				actions.move_cursor_delta(d, actions.scene_side_for_page(page))
			end
		elseif n == 3 then
			if page == "perform" then
				actions.adjust_xfade_delta(d)
			else
				local side = actions.scene_side_for_page(page)
				if side then
					actions.adjust_scene_delta(side, d)
				end
			end
		end
	end

	function Pages.handle_key(n, z)
		local page = actions.current_page()

		if n == 1 then
			if z == 1 then
				interaction.k1_down = true
				interaction.k1_used_as_modifier = false
			else
				interaction.k1_down = false
				if not interaction.k1_used_as_modifier then
					actions.snap_xfade_for_page(page)
				end
			end
		elseif z == 1 and n == 2 then
			actions.change_page(-1)
		elseif z == 1 and n == 3 then
			actions.change_page(1)
		end
	end

	function Pages.draw_perform_page()
		local view = actions.get_perform_view()

		draw_header(view.title)
		draw_crossfader(32, view)

		screen.level(15)
		screen.move(6, 55)
		screen.text(view.effects.A)
		screen.move(122, 55)
		screen.text_right(view.effects.B)
		screen.level(6)
		screen.move(6, 64)
		screen.text(pct(view.amounts.A))
		screen.move(122, 64)
		screen.text_right(pct(view.amounts.B))
	end

	function Pages.draw_scene_page(side)
		local view = actions.get_scene_view(side)

		draw_header(view.title)
		draw_row(18, ui.cursor == 1, "effect", view.effect_label)
		draw_row(28, ui.cursor == 2, "amount", pct(view.amount))
		draw_row(38, ui.cursor == 3, view.param_labels[1], pct(view.param_values.param1))
		draw_row(48, ui.cursor == 4, view.param_labels[2], pct(view.param_values.param2))

		if view.param_count > 2 then
			screen.level(ui.cursor == 5 and 15 or 4)
			screen.move(6, 62)
			screen.text(view.param_labels[3])
			screen.move(48, 62)
			screen.text(short_pct(view.param_values.param3))
			screen.level(ui.cursor == 6 and 15 or 4)
			screen.move(74, 62)
			screen.text(view.param_labels[4])
			screen.move(122, 62)
			screen.text_right(short_pct(view.param_values.param4))
		end
	end

	function Pages.redraw()
		screen.clear()
		local page = actions.current_page()
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
