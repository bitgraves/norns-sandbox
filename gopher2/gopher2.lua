-- gopher2

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Gopher2'
mid = nil
local MPD218
local attack = 0.001

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "sparrowAmp", min = 0.001, max = 1, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "combs" })
  BGUtil.addEngineControlParam(params, {
    id = "attack",
    min = 0.001,
    max = 2,
    warp = 'exp',
    action = function(x)
      attack = x
    end,
  })
  BGUtil.addEngineControlParam(params, { id = "chirp" })
  BGUtil.addEngineControlParam(params, { id = "monitorAmp", min = 0.001, max = 1, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "tilt" })

  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'sparrowAmp',
    [9] = 'combs',
    [12] = 'attack',
    [13] = 'chirp',
    [14] = 'monitorAmp',
    [15] = 'tilt',
  })

  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('sparrowAmp', delta)
  elseif nEnc == 3 then
    params:delta('combs', delta)
  end
end

function key(...)
  BGUtil.setlist_key('gopher2/gopher2', ...)
end

function midiEvent(data)
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local pad = d.note - 36
    local col = pad % 4
    local row = math.floor(pad / 4)
    local harm1 = (row * 2) + 1
    local harm2 = (col * 2) + 1
    engine.trig(1)
    engine.gopherNote(harm1, harm2, attack, 1)
    redraw('h' .. harm1 .. ' h' .. harm2)
  elseif d.type == 'note_off' then
    engine.trig(0)
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
