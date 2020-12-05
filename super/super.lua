-- super

local BGUtil = include('bitgraves/common/bgutil')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Super'
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input

  params:add_control("amp", "amp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("amp", function(x)
    engine.amp(x)
  end)
  
  params:add_control("formFreq", "formFreq", controlspec.new(0, 1, 'lin', 0, 0, 'hz'))
  params:set_action("formFreq", function(x)
    engine.formFreq(util.linexp(0, 1, 400, 4000, x))
  end)
  
  params:add_control("bwmul", "bwmul", controlspec.new(0, 1, 'lin', 0, 1, ''))
  params:set_action("bwmul", function(x)
    engine.bwmul(util.linlin(0, 1, 1, 2, x))
  end)

  params:add_control("formmul", "formmul", controlspec.new(0, 1, 'lin', 0, 1, ''))
  params:set_action("formmul", function(x)
    engine.formmul(util.linlin(0, 1, 1, 5000, x))
  end)
  
  params:add_control("subatk", "subatk", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("subatk", function(x)
    engine.subatk(x)
  end)
  
  params:add_control("duckRelease", "duckRelease", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("duckRelease", function(x)
    engine.duckRelease(util.linlin(0, 1, 2.2, 0.2, x))
  end)
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

-- expose a couple params via enc for debugging
function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('formmul', delta)
  elseif nEnc == 3 then
    params:delta('formFreq', delta)
  end
end

function key(...)
  BGUtil.setlist_key('super/super', ...)
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'formFreq',
  [9] = 'bwmul',
  [12] = 'formmul',
  [13] = 'subatk',
  [14] = 'monitor',
  [15] = 'amp',
  [16] = 'duckRelease',
}

local ccHandlers = {
  ['formFreq'] = function(val)
      params:set('formFreq', val)
      return 'formFreq ' .. tostring(val)
    end,
  ['bwmul'] = function(val)
      params:set('bwmul', val)
      return 'bwmul ' .. val
    end,
  ['formmul'] = function(val)
      params:set('formmul', val)
      return 'form mul ' .. val
    end,
  ['subatk'] = function(val)
      params:set('subatk', val)
      return 'sub attack ' .. val
    end,
  ['monitor'] = function(val)
      params:set('monitor', val)
      return 'monitor ' .. val
    end,
  ['amp'] = function(val)
      params:set('amp', val)
      return 'amp ' .. val
    end,
  ['duckRelease'] = function(val)
      params:set('duckRelease', val)
      return 'sidechain release ' .. val
    end,
}

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    engine.formantIndex(note)
    engine.gate(1)
    if note == 0 then
      engine.trigFreq(0.8)
      redraw('gate on')
    elseif note == 1 then
      engine.trigFreq(0)
      redraw('gate off')
    end
  elseif d.type == 'note_off' then
    engine.gate(0)
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
