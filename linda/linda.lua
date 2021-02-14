-- linda

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Linda'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, {
    id = "noteOffset",
    min = 4,
    max = 24,
    action = function(x) engine.noteOffset(x * -1) end, -- param won't work when max is -24 for some reason
  })
  BGUtil.addEngineControlParam(params, { id = "filterCreep" })
  BGUtil.addEngineControlParam(params, { id = "shudderDuration" })
  
  params:add_control('monitor', 'monitor', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('monitor', function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [9] = 'noteOffset',
    [12] = 'filterCreep',
    [13] = 'shudderDuration',
    [14] = 'monitor',
    [15] = 'amp',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('processing/linda', ...)
end

function midiEvent(data)
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    engine.noteOn(note)
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
