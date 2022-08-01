-- bigfish

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Bigfish'
mid = nil
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, {
    id = "feedbackRez",
    action = function(x)
      local filterRadius = util.linlin(0, 1, 0.99, 0.9993, x)
      local noise = util.linlin(0, 1, -1, -0.2, x) * -1
      engine.filterRadius(filterRadius)
      engine.noise(noise)
    end,
  })
  BGUtil.addEngineControlParam(params, { id = "detune", min = 0, max = 0.08 })
  BGUtil.addEngineControlParam(params, { id = "grainFreqMul", min = 1, max = math.pow(2.0, 7.0 / 12.0) })
  BGUtil.addEngineControlParam(params, { id = "paper", warp = 'exp' })
  
  params:add_control('monitor', 'monitor', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('monitor', function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'feedbackRez',
    [9] = 'grainFreqMul',
    [12] = 'detune',
    [13] = 'paper',
    [14] = 'monitor',
    [15] = 'amp',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('bigfish/bigfish', ...)
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
  Hexagon:draw(MPD218, msg)
end
