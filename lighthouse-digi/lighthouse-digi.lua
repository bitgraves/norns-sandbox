-- lighthouse-digi
-- lighthouse, with digi perc (not hw)
-- requires Vowel quark

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'LighthouseDigi'
mid = nil

local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "vowel" })
  BGUtil.addEngineControlParam(params, { id = "noise", min= 0.2, max = 2 })
  BGUtil.addEngineControlParam(params, { id = "ana" })
  BGUtil.addEngineControlParam(params, { id = "basis", max = 16 })
  BGUtil.addEngineControlParam(params, { id = "kick" })
  BGUtil.addEngineControlParam(params, { id = "click", max = 2.2 })
  BGUtil.addEngineControlParam(params, { id = "clap" })
  BGUtil.addEngineControlParam(params, { id = "amp" })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'vowel',
    [9] = 'noise',
    [12] = 'ana',
    [13] = 'basis',
    [14] = 'monitor',
    [15] = 'amp',
    [16] = 'kick',
    [18] = 'click',
    [19] = 'basis',
    [20] = 'clap',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('lighthouse-digi/lighthouse-digi', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    if index < 17 then
      local sustain = util.linlin(0, 16, 0, 1, index)
      engine.sustain(sustain)
      redraw("sustain " .. tostring(sustain))
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
