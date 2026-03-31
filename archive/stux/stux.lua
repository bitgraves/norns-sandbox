-- stux

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Stux'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, { id = "percAmp" })
  BGUtil.addEngineControlParam(params, {
    id = "rhythm",
    max = 20,
    action = function(x)
      engine.rhythm(math.floor(x))
    end,
  })
  BGUtil.addEngineControlParam(params, { id = "attack", min = 0.01, max = 0.1 })
  BGUtil.addEngineControlParam(params, { id = "release", min = 0.01, max = 3 })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'percAmp',
    [9] = 'rhythm',
    [12] = 'attack',
    [13] = 'release',
    [14] = 'monitor',
    [15] = 'amp',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('percAmp', delta)
  elseif nEnc == 3 then
    params:delta('attack', delta)
  end
end

function key(...)
  BGUtil.setlist_key('stux/stux', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    engine.note(note);
    engine.trig(1);
  elseif d.type == 'note_off' then
    engine.trig(0);
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