-- truths

local BGUtil = include('bitgraves/common/bgutil')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Truths'
mid = nil

local indexOffset = 2

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input

  params:add_control("bend", "bend", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("bend", function(x)
    engine.bend(util.linlin(0, 1, 0, -2, x))
  end)
  
  params:add_control("carrierSource", "carrierSource", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("carrierSource", function(x)
    engine.carrierInAmp(util.linlin(0, 1, 1, 0, x))
    engine.carrierNoiseAmp(util.linlin(0, 1, 0.2, 1, x))
  end)
  
  params:add_control("modulatorSource", "modulatorSource", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("modulatorSource", function(x)
    engine.modSource(x)
  end)
  
  params:add_control("sustain", "sustain", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("sustain", function(x)
    engine.sustain(x)
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
  [3] = 'bend',
  [9] = 'carrierSource',
  [12] = 'modulatorSource',
  [13] = 'sustain',
  [14] = 'monitor',
  [15] = 'amp',
  [16] = 'padOffset',
}

local ccHandlers = {
  ['bend'] = function(val)
    params:set('bend', val)
    return 'bend ' .. val
  end,
  ['carrierSource'] = function(val)
    params:set('carrierSource', val)
    return 'carrier noise ' .. val
  end,
  ['modulatorSource'] = function(val)
    params:set('modulatorSource', val)
    return 'in mod ' .. val
  end,
  ['sustain'] = function(val)
    params:set('sustain', val)
    return 'sustain ' .. val
  end,
  ['monitor'] = function(val)
    params:set('monitor', val)
    return 'monitor ' .. val
    end,
  ['amp'] = function(val)
    params:set('amp', val)
    return 'amp ' .. val
  end,
  ['padOffset'] = function(val)
    indexOffset = math.floor(util.linlin(0, 1, 0, 12, val))
    return 'pad offset ' .. indexOffset
  end,
}

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    engine.noteOn(index + indexOffset)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    engine.noteOff(index + indexOffset)
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
