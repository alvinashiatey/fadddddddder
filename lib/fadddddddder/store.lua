return function(deps)
	local tabutil = deps.tabutil
	local clamp = deps.clamp
	local num_scenes = deps.num_scenes
	local data_dir = deps.data_dir
	local bank_file = deps.bank_file
	local clone_slot = deps.clone_slot
	local sanitize_slot = deps.sanitize_slot

	local SAVE_VERSION = 2

	local Store = {
		save_pending = false,
		contract = {
			bank_file = bank_file,
			save_version = SAVE_VERSION,
			legacy_flat_bank_version = 1,
		},
	}

	local function reset_persisted(persisted)
		persisted.bank = { A = {}, B = {} }
		persisted.slots = { A = 1, B = 1 }
		persisted.xfade = 0.0
		return persisted
	end

	local function save_payload(persisted)
		return {
			version = SAVE_VERSION,
			slots = persisted.slots,
			xfade = persisted.xfade,
			bank = persisted.bank,
		}
	end

	-- Persistence contract:
	-- version 2 payloads save { version, slots, xfade, bank }.
	-- version 1/legacy payloads may present a flat bank array instead of bank.A/bank.B;
	-- those are migrated by duplicating the shared slots into both lanes.
	-- If sanitizing the loaded payload fails, the store deletes the bank file,
	-- resets persisted state to defaults, and immediately writes a fresh v2 bank.
	function Store.ensure_bank(persisted)
		if type(persisted.bank) ~= "table" then
			persisted.bank = { A = {}, B = {} }
		end

		local a_is_table = type(persisted.bank.A) == "table"
		local b_is_table = type(persisted.bank.B) == "table"
		if not a_is_table or not b_is_table then
			local shared = persisted.bank
			persisted.bank = { A = {}, B = {} }
			for i = 1, num_scenes do
				local src = type(shared[i]) == "table" and shared[i] or nil
				persisted.bank.A[i] = clone_slot(src, i)
				persisted.bank.B[i] = clone_slot(src, i)
			end
		end

		for i = 1, num_scenes do
			persisted.bank.A[i] = sanitize_slot(persisted.bank.A[i], i)
			persisted.bank.B[i] = sanitize_slot(persisted.bank.B[i], i)
		end

		persisted.slots.A = clamp(tonumber(persisted.slots.A) or 1, 1, num_scenes)
		persisted.slots.B = clamp(tonumber(persisted.slots.B) or 1, 1, num_scenes)
		persisted.xfade = clamp(tonumber(persisted.xfade) or 0, 0, 1)
		return persisted
	end

	function Store.save_bank(persisted)
		tabutil.save(save_payload(persisted), bank_file)
		Store.save_pending = false
	end

	function Store.request_save()
		Store.save_pending = true
	end

	function Store.load_bank(persisted)
		os.execute("mkdir -p " .. data_dir)
		local ok, data = pcall(tabutil.load, bank_file)
		if ok and type(data) == "table" then
			persisted.slots = type(data.slots) == "table" and data.slots or persisted.slots
			persisted.xfade = data.xfade or persisted.xfade
			persisted.bank = type(data.bank) == "table" and data.bank or persisted.bank
		end
		local ok2, err = pcall(function()
			Store.ensure_bank(persisted)
		end)
		if not ok2 then
			print("fadddddddder: bank load failed (" .. tostring(err) .. "), resetting to defaults")
			os.remove(bank_file)
			reset_persisted(persisted)
			Store.ensure_bank(persisted)
		end
		Store.save_bank(persisted)
		return persisted
	end

	return Store
end
