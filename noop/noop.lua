-- noop

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Noop'
mid = nil

local MPD218

function init()
  BGUtil.configureSystemStuff()
  BGMidi.sendMapping("tanzbar", engine.addMidiMapping)
  
  BGUtil.addEngineControlParam(params, { id = "rez" })
  -- BGUtil.addEngineControlParam(params, { id = "clip" })
  BGUtil.addEngineControlParam(params, { id = "polyNoise" })
  BGUtil.addEngineControlParam(params, { id = "duck" })
  BGUtil.addEngineControlParam(params, { id = "kick" })
  BGUtil.addEngineControlParam(params, { id = "duck" })
  BGUtil.addEngineControlParam(params, { id = "drumsMonitorGain", min = 1, max = 2 })
  BGUtil.addEngineControlParam(params, { id = "kickRamp" })
  BGUtil.addEngineControlParam(params, { id = "percLpf", min = 80, max = 20000, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "amp" })

  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'rez',
    [9] = 'percLpf',
    [12] = 'polyNoise',
    [13] = 'duck',
    [14] = 'monitor',
    [15] = 'amp',
    [16] = 'kick',
    -- [17] = 'clip',
    [18] = 'drumsMonitorGain',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('noop/noop', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    if index < 16 then
      local harm = util.linlin(0, 15, 1, 4, index);
      engine.harm(harm);
      redraw("harm " .. harm)
    elseif index > 31 then
      local midiDeviceIndex = index - 32
      engine.connectMidi(midiDeviceIndex)
      redraw("try midi: " .. tostring(midiDeviceIndex))
    end
    -- engine.noteOn(index)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    -- engine.noteOff(index)
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
