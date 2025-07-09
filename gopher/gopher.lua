-- gopher

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Gopher'
local mix = 0
mid = nil
local MPD218

-- tune to, for example, A
  local tuneFreq = 55 * math.pow(2.0, -4.0 / 12.0)

function init()
  BGUtil.configureSystemStuff()
  
  BGUtil.addEngineControlParam(params, { id = "gate", min = 0, max = 1 })
  BGUtil.addEngineControlParam(params, { id = "sineFreq", min = 18, max = 18000, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "sineAmp1", min = 0.0001, max = 2.0, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "sineHarm1", min = 1, max = 12, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "sineAmp2", min = 0.0001, max = 2.0, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "sineHarm2", min = 1, max = 12, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "release", min = 3, max = 30, warp = 'exp' })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'sineAmp1',
    [9] = 'sineAmp2',
    [12] = 'sineHarm1',
    [13] = 'sineHarm2',
    [14] = 'monitor',
    [15] = 'release',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

-- expose a couple params via enc for debugging
function enc(nEnc, delta)
  if nEnc == 2 then
    
  elseif nEnc == 3 then
    
  end
end

function key(...)
  BGUtil.setlist_key('gopher/gopher', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    if note < 4 then
      local newFreq = tuneFreq * math.pow(2.0, note)
      params:set('sineFreq', newFreq)
      redraw('sineFreq ' .. newFreq)
    end
    params:set('gate', 1)
  elseif d.type == 'note_off' then
    params:set('gate', 0)
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
