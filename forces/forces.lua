-- forces

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Forces'
mid = nil

local indexOffset = 2
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "peaks", min = 32, max = 1 })
  BGUtil.addEngineControlParam(params, { id = "seqDur", min = 0.05, max = 0.17 })
  BGUtil.addEngineControlParam(params, { id = "noise", max = 0.1 })
  BGUtil.addEngineControlParam(params, { id = "freqbase", min = 1, max = 2 })
  BGUtil.addEngineControlParam(params, { id = "freqPowerBase", min = 1, max = 2 })
  BGUtil.addEngineControlParam(params, { id = "kickGain" })
  BGUtil.addEngineControlParam(params, { id = "noiseGain" })
  BGUtil.addEngineControlParam(params, { id = "amp" })

  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(util.linlin(0, 1, 0, 0.7, x))
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'peaks',
    [9] = 'seqDur',
    [12] = 'freqPowerBase',
    [13] = 'freqbase',
    [14] = 'monitor',
    [15] = 'amp',
    [16] = 'kickGain',
    [17] = 'seqDur',
    [18] = 'noiseGain',
    [19] = 'noise',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('forces/forces', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    if index <= 11 then
      engine.filterNoteOn(index)
      redraw('freq mult ' .. index)
    elseif index > 11 and index < 16 then
      local param = math.pow(2, index - 13)
      engine.kickSeqMul(param)
      redraw('kick seq mult ' .. param)
    end
    -- engine.noteOn(index + indexOffset)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    if index < 11 then
      engine.filterNoteOff(0)
    end
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
