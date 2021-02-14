-- baaka

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Baaka'
local mix = 0
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, {
    id = "mix",
    action = function(x)
      mix = x
      engine.mix(x)
    end,
  })
  BGUtil.addEngineControlParam(params, { id = "noiseFreq", min = 1000, max = 18000, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "seqFreq", min = 3, max = 51 })
  BGUtil.addEngineControlParam(params, { id = "oscFreq", min = 110, max = 440, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "bend" })
  BGUtil.addEngineControlParam(params, { id = "amp" })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'bend',
    [9] = 'oscFreq',
    [12] = 'mix',
    [13] = 'noiseFreq',
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
    params:delta('mix', delta)
  elseif nEnc == 3 then
    params:delta('noiseFreq', delta)
  end
end

function key(...)
  BGUtil.setlist_key('baaka/baaka', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    if note == 0 then
      params:set('mix', 0)
      redraw('noise')
    else
      local newSeqFreq = (note * 3.0) + math.random(0, 3)
      params:set('mix', mix)
      params:set('seqFreq', newSeqFreq)
      redraw('seq ' .. newSeqFreq)
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