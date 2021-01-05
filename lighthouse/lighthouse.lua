-- lighthouse
-- requires Vowel quark

local BGUtil = include('bitgraves/common/bgutil')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Lighthouse'
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input
  audio.level_monitor(0) -- just reset for now...

  params:add_control("vowel", "vowel", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("vowel", function(x)
    engine.vowel(x)
  end)
  
  params:add_control("noise", "noise", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("noise", function(x)
    engine.noise(util.linlin(0, 1, 0, 2, x))
  end)
  
  params:add_control("ana", "ana", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("ana", function(x)
    engine.ana(x)
  end)
  
  params:add_control("basis", "basis", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("basis", function(x)
    engine.basis(util.linlin(0, 1, 0, 16, x))
  end)
  
  params:add_control("kick", "kick", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("kick", function(x)
    engine.kick(x)
  end)
  
  params:add_control("click", "click", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("click", function(x)
    engine.click(util.linlin(0, 1, 0, 2, x))
  end)
  
  params:add_control("clap", "clap", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("clap", function(x)
    engine.clap(x)
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

osc.event = function(path, args, from)
  print('rec osc: ' .. tostring(path))
  print(' from: ' .. tostring(from.host) .. ':' .. tostring(from.port))
end

function key(...)
  BGUtil.setlist_key('lighthouse/lighthouse', ...)
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
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
}

local ccHandlers = {
  ['vowel'] = function(val)
    params:set('vowel', val)
    return 'vowel ' .. val
  end,
  ['noise'] = function(val)
    params:set('noise', val)
    return 'noise ' .. val
  end,
  ['ana'] = function(val)
    params:set('ana', val)
    return 'ana ' .. val
  end,
  ['basis'] = function(val)
    params:set('basis', val)
    return 'basis ' .. val
  end,
  ['kick'] = function(val)
    params:set('kick', val)
    return 'kick ' .. val
  end,
  ['click'] = function(val)
    params:set('click', val)
    return 'click ' .. val
  end,
  ['clap'] = function(val)
    params:set('clap', val)
    return 'clap ' .. val
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
