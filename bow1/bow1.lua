-- bow1

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Bow1'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "mix" })
  BGUtil.addEngineControlParam(params, { id = "velbow" })
  BGUtil.addEngineControlParam(params, { id = "comb", min = 0.0001, max = 1, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "combLerp" })

  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)

  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'mix',
    [9] = 'velbow',
    [12] = 'comb',
    [13] = 'combLerp',
  })

  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('mix', delta)
  elseif nEnc == 3 then
    params:delta('velbow', delta)
  end
end

function key(...)
  BGUtil.setlist_key('bow1/bow1', ...)
end

function midiEvent(data)
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    if note == 0 then
      engine.gate(1)
    elseif note > 0 then
      local combNote = note - 1
      engine.combNote(combNote)
    end
  elseif d.type == 'note_off' then
    local note = d.note - 36
    if note == 0 then
      engine.gate(0)
    end
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
