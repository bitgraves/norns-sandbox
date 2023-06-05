-- crystal

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Crystal'
local mix = 0
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, { id = "bend" })
  BGUtil.addEngineControlParam(params, { id = "glitch" })
  BGUtil.addEngineControlParam(params, { id = "envLen", warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "freqLpf", min = 40, max = 20000, warp = 'exp' })
  
  params:add_control('monitor', 'monitor', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('monitor', function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'bend',
    [9] = 'glitch',
    [12] = 'envLen',
    [13] = 'freqLpf',
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
    params:delta('monitor', delta)
  elseif nEnc == 3 then
    params:delta('bend', delta)
  end
end

function key(n, z)
  if n == 2 then
    if z == 1 then
      engine.noteOn(7)
    else
      engine.noteOff(7)
    end
  end
end

function key(...)
  BGUtil.setlist_key('crystal/crystal', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    engine.noteOn(note)
  elseif d.type == 'note_off' then
    local note = d.note - 36
    engine.noteOff(note)
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