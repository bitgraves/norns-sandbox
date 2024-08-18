-- rabies

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Rabies'
local mix = 0
mid = nil
local MPD218

-- tune to, for example, E (A + 7)
  local tuneFreq = 110 * math.pow(2.0, 7.0 / 12.0)

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
  
  BGUtil.addEngineControlParam(params, { id = "noiseFreq", min = tuneFreq, max = tuneFreq * 2, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "subFreq", min = tuneFreq * 0.5, max = tuneFreq, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "sineFreq", min = 18, max = 18000, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "sineAmp", min = 0.0001, max = 0.1, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "amp" })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'sineAmp',
    [9] = 'subFreq',
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
  BGUtil.setlist_key('rabies/rabies', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    local newFreq = tuneFreq * math.pow(2.0, note / 12.0)
    params:set('sineFreq', newFreq)
    redraw('sineFreq ' .. newFreq)
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
