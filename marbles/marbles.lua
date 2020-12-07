-- marbles

local BGUtil = include('bitgraves/common/bgutil')
local Hexagon = include('bitgraves/common/hexagon')
local MusicUtil = require 'musicutil'

engine.name = 'Marbles'
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input
  
  params:add_control("lowMonitor", "lowMonitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("lowMonitor", function(x)
    engine.lowMonitorLpf(util.linexp(0, 1, 100, 2000, x))
    engine.lowMonitorAmp(x)
  end)

  params:add_control("carrierNoise", "carrierNoise", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("carrierNoise", function(x)
    engine.carrierNoise(x)
  end)
  
  params:add_control("ana", "ana", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("ana", function(x)
    engine.ana(x)
  end)
  
  params:add_control("lag", "lag", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("lag", function(x)
    engine.lag(x)
  end)
  
  params:add_control("freq", "freq", controlspec.new(110, 10000, 'exp', 1, 110, 'Hz'))
  params:set_action("freq", function(x)
    engine.freq(x)
  end)
  
params:add_control("spread", "spread", controlspec.new(1, 5, 'lin', 1, 1, ''))
  params:set_action("spread", function(x)
    engine.spread(x)
  end)

  params:add_control("amp", "amp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("amp", function(x)
    engine.amp(x)
  end)
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function enc(nEnc, delta)

end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'lowMonitor',
  [9] = 'carrierNoise',
  [12] = 'ana',
  [13] = 'lag',
  [14] = 'monitor',
  [15] = 'amp',
}

local ccHandlers = {
  ['lowMonitor'] = function(val)
    params:set('lowMonitor', val)
    local freq = util.linexp(0, 1, 100, 2000, val)
    return 'lo monitor ' .. util.round(freq, 1) .. ' ' .. util.round(val, 0.01)
  end,
  ['carrierNoise'] = function(val)
    params:set('carrierNoise', val)
    return 'carrier noise ' .. val
  end,
  ['ana'] = function(val)
    params:set('ana', val)
    return 'dig -> ana ' .. val
  end,
  ['lag'] = function(val)
    params:set('lag', val)
    return 'lag ' .. val
  end,
  ['monitor'] = function(val)
    params:set('monitor', val)
    return 'monitor ' .. val
    end,
  ['amp'] = function(val)
    params:set('amp', val)
    return 'amp ' .. val
  end,
}

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
    local handler = ccAkaiMapping[d.cc]
    if handler ~= nil and ccHandlers[handler] ~= nil then
      local msg = ccHandlers[handler](d.val / 127)
      redraw(msg)
    end
  end
end

function redraw(msg)
  Hexagon:draw(msg, ccAkaiMapping)
end
