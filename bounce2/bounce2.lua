-- bounce

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

local MusicUtil = require 'musicutil'

engine.name = 'Bounce2'

mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, { id = "phaseDrift", max = 5 })
  BGUtil.addEngineControlParam(params, {
    id = "envDepth",
    min = -1,
    max = -0.1,
    action = function(x) engine.envDepth(x * -1) end
  })
  BGUtil.addEngineControlParam(params, {
    id = "stableFreq",
    min = 0.5,
    max = 200 * MusicUtil.interval_to_ratio(-4),
    warp = 'exp',
  })
  BGUtil.addEngineControlParam(params, { id = "noise", max = 0.4 })
  BGUtil.addEngineControlParam(params, { id = "lpf", min = 60, max = 20000, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "release", min = -10, max = -2, warp = 'exp', action = function(x) engine.release(x * -1) end })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'release',
    [16] = 'phaseDrift',
    [9] = 'noise',
    [12] = 'stableFreq',
    [13] = 'lpf',
    [14] = 'monitor',
    [15] = 'amp',
    [17] = 'envDepth',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

-- expose a couple params via enc for debugging
function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('bounce', delta)
  elseif nEnc == 3 then
    params:delta('lpf', delta)
  end
end

function key(...)
  BGUtil.setlist_key('bounce/bounce', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    local minCombFreq = 55 * MusicUtil.interval_to_ratio(-4)
    local triFreq = 13.75 * 0.5 * MusicUtil.interval_to_ratio(index)
    local combFreq = minCombFreq * MusicUtil.interval_to_ratio(index)
    engine.initialFreq(triFreq)
    engine.combFreq(combFreq)
    engine.gate(1)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    engine.gate(0)
  elseif d.type == 'cc' then
    local handled, msg = BGMidi.handleCCMPD218(MPD218, params, d.cc, d.val)
    if handled then
      redraw(msg)
    end
  end
end

function redraw(msg)
  Hexagon:draw(MPD218, msg)
end