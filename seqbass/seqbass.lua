-- seqbass

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Seqbass'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "attack", min = 0.001, max = 0.2, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "fold", min = 1, max = 20 })
  BGUtil.addEngineControlParam(params, { id = "env" })
  BGUtil.addEngineControlParam(params, { id = "mix" })
  BGUtil.addEngineControlParam(params, { id = "hpf", min = 30, max = 18000, warp = 'exp' })
  BGUtil.addEngineControlParam(params, { id = "voxGain", min = 0.001, max = 2, warp = 'exp' })

  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)

  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'attack',
    [9] = 'fold',
    [12] = 'voxGain',
    [13] = 'hpf',
    [14] = 'env',
    [15] = 'mix',
  })

  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('fold', delta)
  elseif nEnc == 3 then
    params:delta('env', delta)
  end
end

function key(...)
  BGUtil.setlist_key('seqbass/seqbass', ...)
end

function midiEvent(data)
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local pad = d.note - 36
    if pad < 12 then
      local segment = util.linlin(0, 3, 0.02, 0.05, pad % 4)
      local row = math.floor(pad / 4)
      local tone = util.linexp(0, 3, 0.5, 4, row)
      engine.voxTone(tone)
      engine.voxSegment(segment)
      redraw('tone ' .. string.format('%.2f', tone))
    else
      local noteidx = pad - 12
      local combnote = ({[-1] = -2, [0] = -2, [1] = 0, [2] = 1, [3] = 7})[noteidx] or 0
      engine.voxCombnote(combnote)
      redraw('comb ' .. combnote)
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
