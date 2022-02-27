-- unicorn

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Unicorn'
mid = nil

local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "noise", max = 0.75 })
  BGUtil.addEngineControlParam(params, {
    id = "shift",
    action = function(x) engine.shift(math.floor(x + 0.5)) end
  })
  BGUtil.addEngineControlParam(params, {
    id = "freq",
    min = 180,
    max = 18000,
    warp = 'exp',
  })
  BGUtil.addEngineControlParam(params, { id = "chord" })
  BGUtil.addEngineControlParam(params, { id = "kick" })
  BGUtil.addEngineControlParam(params, { id = "hat", max = 2})
  BGUtil.addEngineControlParam(params, { id = "clap", max = 2 })
  BGUtil.addEngineControlParam(params, { id = "amp" })
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'noise',
    [9] = 'shift',
    [12] = 'freq',
    [13] = 'chord',
    [14] = 'monitor',
    [15] = 'amp',
    [16] = 'kick',
    [18] = 'hat',
    [20] = 'clap',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('unicorn/unicorn', ...)
end

function midiEvent(data)
  -- tab.print(midi.to_msg(data))
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local index = d.note - 36
    if (index < 4) then
      if index == 0 then engine.modNoise(0) end
      if index == 1 then engine.modShift(0) end
      if index == 2 then engine.modFreq(0) end
      if index == 3 then engine.percAmp(0) end
    elseif (index < 8) then
      local param = 1 + ((index - 4) * 0.33)
      redraw("tempo " .. param)
      engine.tempo(param)
    end
    engine.noteOn(index)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    if (index < 4) then
      if index == 0 then engine.modNoise(1) end
      if index == 1 then engine.modShift(1) end
      if index == 2 then engine.modFreq(1) end
      if index == 3 then engine.percAmp(1) end
    elseif (index < 8) then
    elseif (index < 16) then
      if index == 8 then engine.note1(-5) end
      if index == 9 then engine.note1(-2) end
      if index == 10 then engine.note1(0.001) end
      if index == 11 then engine.note1(2) end
      if index == 12 then engine.note2(7) end
      if index == 13 then engine.note2(10) end
      if index == 14 then engine.note2(12) end
      if index == 15 then engine.note2(14) end
    end
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
