-- truths

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Truths'
mid = nil

local indexOffset = 2
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, {
    id = "bend",
    max = 2,
    action = function(x)
      engine.bend(x * -1) -- max < 1 fails for some reason
    end,
  })
  BGUtil.addEngineControlParam(params, { id = "modSource" })
  BGUtil.addEngineControlParam(params, { id = "sustain" })
  BGUtil.addEngineControlParam(params, {
    id = "carrierSource",
    action = function(x)
      engine.carrierInAmp(util.linlin(0, 1, 1, 0, x))
      engine.carrierNoiseAmp(util.linlin(0, 1, 0.2, 1, x))
    end,
  })

  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'bend',
    [9] = 'carrierSource',
    [12] = 'modSource',
    [13] = 'sustain',
    [14] = 'monitor',
    [15] = 'amp',
    -- [16] = 'padOffset',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    engine.noteOn(index + indexOffset)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    engine.noteOff(index + indexOffset)
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
