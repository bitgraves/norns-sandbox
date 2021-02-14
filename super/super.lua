-- super

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Super'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, { id = "formFreq", min = 400, max = 4000, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "bwmul", min = 1, max = 2 })
  BGUtil.addEngineControlParam(params, { id = "formmul", min = 1, max = 5000 })
  BGUtil.addEngineControlParam(params, { id = "subatk" })
  BGUtil.addEngineControlParam(params, {
    id = "duckRelease",
    min = -2.2, max = -0.2,
    action = function(x)
      engine.duckRelease(x * -1)
    end,
  })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'formFreq',
    [9] = 'bwmul',
    [12] = 'formmul',
    [13] = 'subatk',
    [14] = 'monitor',
    [15] = 'amp',
    [16] = 'duckRelease',
  })
  
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
    local handled, msg = BGMidi.handleCCMPD218(MPD218, params, d.cc, d.val)
    if handled then
      redraw(msg)
    end
  end
end

function redraw(msg)
  Hexagon:drawFancy(MPD218, msg)
end
