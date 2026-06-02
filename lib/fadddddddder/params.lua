return function(deps)
	local params = deps.params
	local on_change = deps.on_change

	local Params = {}

	function Params.register()
		params:add_separator("fadddddddder_delay", "fadddddddder delay")
		params:add_control("delay_bpm", "delay bpm", controlspec.new(40, 240, "lin", 1, 120, "bpm"))
		params:set_action("delay_bpm", on_change)
		params:add_option("delay_sync", "delay sync", { "free", "sync" }, 2)
		params:set_action("delay_sync", on_change)
	end

	return Params
end
