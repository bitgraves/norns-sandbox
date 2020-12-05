-- tencho
-- is techno spelled wrong

local BGUtil = include('bitgraves/common/bgutil')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Tencho'
mid = nil

local indexOffset = 2

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_stereo() -- expect stereo input

  params:add_control("amp", "amp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("amp", function(x)
    engine.amp(x)
  end)
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  params:add_control("chopR", "chopR", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("chopR", function(x)
    engine.chopR(x)
  end)
  
  params:add_control("chopTimeR", "chopTimeR", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("chopTimeR", function(x)
    engine.chopTimeR(util.linlin(0, 1, 0.3, 0.7, x))
  end)
  
  params:add_control("delayR", "delayR", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("delayR", function(x)
    engine.delayR(x)
  end)
  
  params:add_control("noise", "noise", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("noise", function(x)
    engine.noise(util.linlin(0, 1, 0, 0.25, x))
  end)
  
  --[[
    this.addCommand("chopR", "f", {|msg|
      sCarrier2.set(\chop, msg[1]);
    });
    this.addCommand("delayTime", "f", {|msg|
      sCarrier1.set(\delayFeedbackTime, msg[1]);
      sCarrier2.set(\delayFeedbackTime, msg[1]);
    });
  ]]
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function enc(nEnc, delta)

end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'noise',
  [9] = 'delayR',
  [12] = 'chopR',
  [13] = 'chopTimeR',
  [14] = 'monitor',
  [15] = 'amp',
}

local ccHandlers = {
  ['noise'] = function(val)
    params:set('noise', val)
    return 'noise ' .. val
  end,
  ['chopR'] = function(val)
    params:set('chopR', val)
    return 'chopR ' .. val
  end,
  ['chopTimeR'] = function(val)
    params:set('chopTimeR', val)
    return 'chop time ' .. val
  end,
  ['delayR'] = function(val)
    params:set('delayR', val)
    return 'delayR ' .. val
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
    if index < 4 then
      local val = 0.1 * (index + 1)
      engine.delayTime(val)
      redraw('delay time ' .. val)
    end
  elseif d.type == 'note_off' then
    local index = d.note - 36
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
