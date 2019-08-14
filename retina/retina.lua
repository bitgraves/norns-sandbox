-- retina
-- note: uses custom monitor synth

local BGUtil = dofile(_path.code .. 'bitgraves/common/bgutil.lua')
local Hexagon = BGUtil.dofile_norns('common/hexagon.lua')

engine.name = 'Retina'
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input

  params:add_control("amp", "amp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("amp", function(x)
    engine.amp(x)
  end)
  
  params:add_control("speed", "speed", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("speed", function(x)
    engine.speed(util.linlin(0, 1, 1, 2.5, x))
  end)

  params:add_control("delayAmp", "delayAmp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("delayAmp", function(x)
    engine.delayAmp(util.linlin(0, 1, 1, 0, x))
  end)
  
  params:add_control("destroy", "destroy", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("destroy", function(x)
    engine.destroy(util.linlin(0, 1, 125.0 / 127.0, 0, x))
  end)
  
  params:add_control("sidechainMonitor", "sidechainMonitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("sidechainMonitor", function(x)
    engine.sidechainMonitor(x)
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
    -- TODO params:delta('formmul', delta)
  elseif nEnc == 3 then
    --TODO params:delta('formFreq', delta)
  end
end

function key(...)
  BGUtil.setlist_key('retina/retina', ...)
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'speed',
  [9] = 'delayAmp',
  [13] = 'destroy',
  [14] = 'sidechainMonitor', -- custom monitor synth
  [15] = 'amp',
  [20] = 'monitor', -- standard monitor on nonstandard knob
}

local ccHandlers = {
  ['speed'] = function(val)
      params:set('speed', val)
      return 'silence ' .. val
    end,
  ['delayAmp'] = function(val)
      params:set('delayAmp', val)
      return 'delay amp ' .. val
    end,
  ['destroy'] = function(val)
      params:set('destroy', val)
      return 'destroy ' .. val
    end,
  ['sidechainMonitor'] = function(val)
      params:set('sidechainMonitor', val)
      return 'minitaur ' .. val
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
    local note = d.note - 36
    if note == 11 then
      engine.speedMul(1.5)
      redraw('faster on')
    else
      engine.noteOn(note)
    end
  elseif d.type == 'note_off' then
    local note = d.note - 36
    if note == 11 then
      engine.speedMul(1)
      redraw('faster off')
    else
      engine.noteOff(note)
    end
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