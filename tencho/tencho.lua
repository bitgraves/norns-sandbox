-- tencho
-- is techno spelled wrong

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Tencho'
mid = nil

local indexOffset = 2
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, { id = "chopR" })
  BGUtil.addEngineControlParam(params, { id = "chopTimeR", min = 0.3, max = 0.7 })
  BGUtil.addEngineControlParam(params, { id = "delayR" })
  BGUtil.addEngineControlParam(params, { id = "noise", max = 0.25 })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'noise',
    [9] = 'delayR',
    [12] = 'chopR',
    [13] = 'chopTimeR',
    [14] = 'monitor',
    [15] = 'amp',
  })
  
  --[[
    this.addCommand("chopR", "f", {|msg|
      sCarrier2.set(\chop, msg[1]);
    });
    this.addCommand("delayTime", "f", {|msg|
      sCarrier1.set(\delayFeedbackTime, msg[1]);
      sCarrier2.set(\delayFeedbackTime, msg[1]);
    });
  ]]
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    if index < 4 then
      local val = 0.1 * (index + 1)
      engine.delayTime(val)
      redraw('delay time ' .. val)
    end
  elseif d.type == 'note_off' then
    local index = d.note - 36
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
