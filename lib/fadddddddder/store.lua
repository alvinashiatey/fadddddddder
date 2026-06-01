return function(deps)
	local tabutil = deps.tabutil
	local clamp = deps.clamp
	local num_scenes = deps.num_scenes
	local data_dir = deps.data_dir
	local bank_file = deps.bank_file
	local clone_slot = deps.clone_slot
	local sanitize_slot = deps.sanitize_slot

	local Store = {
		save_pending = false,
	}

	function Store.ensure_bank(state)
		if type(state.bank) ~= "table" then
			state.bank = { A = {}, B = {} }
		end

		local a_is_table = type(state.bank.A) == "table"
		local b_is_table = type(state.bank.B) == "table"
		if not a_is_table or not b_is_table then
			local shared = state.bank
			state.bank = { A = {}, B = {} }
			for i = 1, num_scenes do
				local src = type(shared[i]) == "table" and shared[i] or nil
				state.bank.A[i] = clone_slot(src, i)
				state.bank.B[i] = clone_slot(src, i)
			end
		end

		for i = 1, num_scenes do
			state.bank.A[i] = sanitize_slot(state.bank.A[i], i)
			state.bank.B[i] = sanitize_slot(state.bank.B[i], i)
		end

		state.slots.A = clamp(tonumber(state.slots.A) or 1, 1, num_scenes)
		state.slots.B = clamp(tonumber(state.slots.B) or 1, 1, num_scenes)
		state.xfade = clamp(tonumber(state.xfade) or 0, 0, 1)
		return state
	end

	function Store.save_bank(state)
		tabutil.save({ version = 2, slots = state.slots, xfade = state.xfade, bank = state.bank }, bank_file)
		Store.save_pending = false
	end

	function Store.request_save()
		Store.save_pending = true
	end

	function Store.load_bank(state)
		os.execute("mkdir -p " .. data_dir)
		local ok, data = pcall(tabutil.load, bank_file)
		if ok and type(data) == "table" then
			state.slots = type(data.slots) == "table" and data.slots or state.slots
			state.xfade = data.xfade or state.xfade
			state.bank = type(data.bank) == "table" and data.bank or state.bank
		end
		local ok2, err = pcall(function()
			Store.ensure_bank(state)
		end)
		if not ok2 then
			print("fadddddddder: bank load failed (" .. tostring(err) .. "), resetting to defaults")
			os.remove(bank_file)
			state.bank = { A = {}, B = {} }
			state.slots = { A = 1, B = 1 }
			state.xfade = 0.0
			Store.ensure_bank(state)
		end
		Store.save_bank(state)
		return state
	end

	return Store
end
