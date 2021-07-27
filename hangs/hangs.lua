-- hangs

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Hangs'
mid = nil

local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "sixthLevel" })
  BGUtil.addEngineControlParam(params, {
    id = "wob",
    min = -0.1,
    max = -0.05,
    warp = 'exp',
    action = function(x) engine.wob(x * -1) end
  })
  BGUtil.addEngineControlParam(params, {
    id = "pulseWidth",
    min = -0.99,
    max = -0.6,
    action = function(x) engine.pulseWidth(x * -1) end
  })
  BGUtil.addEngineControlParam(params, { id = "kick" })
  BGUtil.addEngineControlParam(params, { id = "hat", max = 1.5 })
  BGUtil.addEngineControlParam(params, { id = "click", max = 1.5 })
  BGUtil.addEngineControlParam(params, { id = "amp" })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'sixthLevel',
    [12] = 'wob',
    [13] = 'pulseWidth',
    [14] = 'monitor',
    [15] = 'amp',
    [16] = 'kick',
    [18] = 'hat',
    [20] = 'click',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('hangs/hangs', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    engine.noteOn(index)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    engine.noteOff(index)
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
