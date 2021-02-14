-- retina
-- note: uses custom monitor synth

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Retina'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, { id = "speed", min = 1, max = 2.5 })
  BGUtil.addEngineControlParam(params, {
    id = "delayAmp",
    action = function(x)
      engine.delayAmp(1 - x)
    end,
  })
  BGUtil.addEngineControlParam(params, {
    id = "destroy",
    min = -125.0 / 127.0,
    max = 0,
    action = function(x)
      engine.destroy(x * -1)
    end,
  })
  BGUtil.addEngineControlParam(params, { id = "sidechainMonitor" })

  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'speed',
    [9] = 'delayAmp',
    [13] = 'destroy',
    [14] = 'sidechainMonitor', -- custom monitor synth
    [15] = 'amp',
    [20] = 'monitor', -- standard monitor on nonstandard knob
  })
  
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
    local handled, msg = BGMidi.handleCCMPD218(MPD218, params, d.cc, d.val)
    if handled then
      redraw(msg)
    end
  end
end

function redraw(msg)
  Hexagon:drawFancy(MPD218, msg)
end