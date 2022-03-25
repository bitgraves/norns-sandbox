-- murmur

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Murmur'
mid = nil

local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "vowel" })
  BGUtil.addEngineControlParam(params, {
    id = "harmonic",
    min = 1,
    max = 6,
  })
  BGUtil.addEngineControlParam(params, {
    id = "scale",
    min = 0.2,
    max = 1,
  })
  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, { id = "samp", max = 1 })
  BGUtil.addEngineControlParam(params, {
    id = "sampHpf",
    min = 60,
    max = 9000,
    warp = 'exp',
  })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'vowel',
    [9] = 'scale',
    [12] = 'harmonic',
    [13] = 'samp',
    [14] = 'monitor',
    [15] = 'amp',
    [16] = 'sampHpf',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('murmur/murmur', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    local morphFreq = util.linlin(0, 15, 3, 24, index);
    engine.morphFreq(morphFreq);
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
