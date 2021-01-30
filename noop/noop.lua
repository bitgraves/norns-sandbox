-- noop

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Noop'
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input
  audio.level_monitor(0) -- just reset for now...
  BGMidi.sendMapping("tanzbar", engine.addMidiMapping)
  
  params:add_control("rez", "rez", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("rez", function(x)
    engine.rez(x)
  end)
  
  params:add_control("clip", "clip", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("clip", function(x)
    engine.clip(x)
  end)
  
  params:add_control("polyNoise", "polyNoise", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("polyNoise", function(x)
    engine.polyNoise(x)
  end)
  
  params:add_control("duck", "duck", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("duck", function(x)
    engine.duck(x)
  end)
  
  params:add_control("kick", "kick", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("kick", function(x)
    engine.kick(x)
  end)
  
  params:add_control("click", "click", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("click", function(x)
    engine.click(util.linlin(0, 1, 0, 2, x))
  end)
  
  params:add_control("kickRamp", "kickRamp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("kickRamp", function(x)
    engine.kickRamp(x)
  end)

  params:add_control("amp", "amp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("amp", function(x)
    engine.amp(x)
  end)
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('noop/noop', ...)
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'rez',
  [9] = 'clip',
  [12] = 'polyNoise',
  [13] = 'duck',
  [14] = 'monitor',
  [15] = 'amp',
  [16] = 'kick',
  [17] = 'kickRamp',
  [18] = 'click',
}

local ccHandlers = {
  ['rez'] = function(val)
    params:set('rez', val)
    return 'rez ' .. val
  end,
  ['clip'] = function(val)
    params:set('clip', val)
    return 'clip ' .. val
  end,
  ['polyNoise'] = function(val)
    params:set('polyNoise', val)
    return 'poly ' .. val
  end,
  ['duck'] = function(val)
    params:set('duck', val)
    return 'duck ' .. val
  end,
  ['kick'] = function(val)
    params:set('kick', val)
    return 'kick ' .. val
  end,
  ['click'] = function(val)
    params:set('click', val)
    return 'click ' .. val
  end,
  ['kickRamp'] = function(val)
    params:set('kickRamp', val)
    return 'kick ramp ' .. val
  end,
  ['monitor'] = function(val)
    params:set('monitor', val)
    return 'monitor ' .. val
    end,
  ['amp'] = function(val)
    params:set('amp', val)
    return 'amp ' .. val
  end,
}

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
    local handler = ccAkaiMapping[d.cc]
    if handler ~= nil and ccHandlers[handler] ~= nil then
      local msg = ccHandlers[handler](d.val / 127)
      redraw(msg)
    end
  end
end

function redraw(msg)
  Hexagon:draw(msg, ccAkaiMapping)
end
