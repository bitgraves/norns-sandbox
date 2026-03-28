-- gormant

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Gormant'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "lpf", min = 100, max = 16000, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "padGain" })
  BGUtil.addEngineControlParam(params, { id = "spread", min = 0.5, max = 1, warp = 'exp' })

  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)

  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'lpf',
    [9] = 'padGain',
    [12] = 'spread',
  })

  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('lpf', delta)
  elseif nEnc == 3 then
    params:delta('padGain', delta)
  end
end

function key(...)
  BGUtil.setlist_key('gormant/gormant', ...)
end

function midiEvent(data)
  local d = midi.to_msg(data)
  if d.type == 'cc' then
    local handled, msg = BGMidi.handleCCMPD218(MPD218, params, d.cc, d.val)
    if handled then
      redraw(msg)
    end
  end
end

function redraw(msg)
  Hexagon:draw(MPD218, msg)
end
