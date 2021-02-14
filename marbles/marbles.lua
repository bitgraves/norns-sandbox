-- marbles

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')
local MusicUtil = require 'musicutil'

engine.name = 'Marbles'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()
  
  BGUtil.addEngineControlParam(params, {
    id = "lowMonitor",
    action = function(x)
      engine.lowMonitorLpf(util.linexp(0, 1, 100, 2000, x))
      engine.lowMonitorAmp(x)
    end,
  })
  BGUtil.addEngineControlParam(params, { id = "carrierNoise" })
  BGUtil.addEngineControlParam(params, { id = "ana" })
  BGUtil.addEngineControlParam(params, { id = "lag" })
  BGUtil.addEngineControlParam(params, { id = "freq", controlspec = controlspec.new(110, 10000, 'exp', 1, 110, 'Hz') })
  BGUtil.addEngineControlParam(params, { id = "spread", controlspec = controlspec.new(1, 5, 'lin', 1, 1, '') })
  BGUtil.addEngineControlParam(params, { id = "bend", max = -24 })
  BGUtil.addEngineControlParam(params, { id = "sustain", min = 0.9, max = 0.1 })
  BGUtil.addEngineControlParam(params, { id = "amp" })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'lowMonitor',
    [9] = 'bend',
    [12] = 'ana',
    [13] = 'lag',
    [14] = 'monitor',
    [15] = 'amp',
    [17] = 'sustain',
    [19] = 'carrierNoise',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('marbles/marbles', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    local col = index % 4
    local row = math.floor(index / 4)
    local scale = { 2, 7, 9, 12 };
    local freq = 110 * MusicUtil.interval_to_ratio(scale[col + 1])
    engine.freq(freq)
    engine.spread(1 + row)
    engine.noteOn(index)
    redraw(col .. ', ' .. row)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    engine.noteOff(index)
  elseif d.type == 'cc' then
    local handled, msg = BGMidi.handleCCMPD218(MPD218, params, d.cc, d.val)
    if handled then
      redraw(msg)
    end
  end
end

function redraw(msg)
  Hexagon:drawFancy(MPD218, msg)
end
