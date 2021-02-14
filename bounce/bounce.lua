-- bounce

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Bounce'
local isOther = 0
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, { id = "bounce" })
  BGUtil.addEngineControlParam(params, { id = "drift" })
  BGUtil.addEngineControlParam(params, { id = "lpf" })
  BGUtil.addEngineControlParam(params, { id = "hiTexAmp" })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'hiTexAmp',
    [9] = 'bounce',
    [12] = 'drift',
    [13] = 'lpf',
    [14] = 'monitor',
    [15] = 'amp',
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
    local note = d.note - 36
    if note == 0 then
      engine.other(1)
      isOther = 1
      redraw('')
    else
      engine.drift(note / 16)
      engine.driftTrig(1)
      redraw('sweep trig ' .. note / 16)
    end
  elseif d.type == 'note_off' then
    local note = d.note - 36
    if note == 0 then
      engine.other(0)
      isOther = 0
      redraw('')
    else
      engine.driftTrig(0)
      redraw('stop sweep')
    end
  elseif d.type == 'cc' then
    local handled, msg = BGMidi.handleCCMPD218(MPD218, params, d.cc, d.val)
    if handled then
      redraw(msg)
    end
  end
end

function redraw(msg)
  if isOther == 1 then
    screen.clear()
    screen.level(12)
    screen.move(48, 32)
    screen.text('???????')
    screen.update()
  else
    Hexagon:drawFancy(MPD218, msg)
  end
end