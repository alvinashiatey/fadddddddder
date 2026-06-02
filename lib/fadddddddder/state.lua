local State = {}

function State.new()
	return {
		persisted = {
			xfade = 0.0,
			slots = { A = 1, B = 1 },
			bank = { A = {}, B = {} },
		},
		ui = {
			page_index = 1,
			cursor = 1,
		},
		interaction = {
			k1_down = false,
			k1_used_as_modifier = false,
		},
		runtime = {
			engine_ready = false,
			redraw_metro = nil,
		},
	}
end

return State
