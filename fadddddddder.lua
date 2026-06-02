-- fadddddddder: octo-inspired scene crossfader for live input
-- v0.7.0 @alvinashiatey

engine.name = "Fadddddddder"

local App = include("lib/fadddddddder/app")({
	engine = engine,
	params = params,
	audio = audio,
	clock = clock,
	metro = metro,
	util = util,
	redraw = function()
		redraw()
	end,
})

function init()
	App.init()
end

function enc(n, d)
	App.enc(n, d)
end

function key(n, z)
	App.key(n, z)
end

function redraw()
	App.redraw()
end

function cleanup()
	App.cleanup()
end
