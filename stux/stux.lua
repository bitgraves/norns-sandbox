-- stux

local BGUtil = dofile(_path.code .. 'bitgraves/common/bgutil.lua')
local Hexagon = BGUtil.dofile_norns('common/hexagon.lua')

engine.name = 'Stux'
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input

  params:add_control("amp", "amp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("amp", function(x)
    engine.amp(x)
  end)
  
  params:add_control("percAmp", "percAmp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("percAmp", function(x)
    engine.percAmp(x)
  end)

  params:add_control("rhythm", "rhythm", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("rhythm", function(x)
    engine.rhythm(math.floor(util.linlin(0, 1, 0, 20, x)))
  end)
  
  params:add_control("attack", "attack", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("attack", function(x)
    engine.attack(util.linlin(0, 1, 0.01, 0.1, x))
  end)
  
  params:add_control("release", "release", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("release", function(x)
    engine.release(util.linlin(0, 1, 0.01, 3, x))
  end)
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function cleanup()
  mid.event = nil
  midi.cleanup()
end

function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('percAmp', delta)
  elseif nEnc == 3 then
    params:delta('attack', delta)
  end
end

function key(...)
  BGUtil.setlist_key('stux/stux', ...)
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'percAmp',
  [9] = 'rhythm',
  [12] = 'attack',
  [13] = 'release',
  [14] = 'monitor',
  [15] = 'amp',
}

local ccHandlers = {
  ['percAmp'] = function(val)
      params:set('percAmp', val)
      return 'perc amp ' .. val
    end,
  ['rhythm'] = function(val)
      params:set('rhythm', val)
      return 'rhythm ' .. val
    end,
  ['attack'] = function(val)
      params:set('attack', val)
      return 'attack ' .. val
    end,
  ['release'] = function(val)
      params:set('release', val)
      return 'release ' .. val
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
    local note = d.note - 36
    engine.note(note);
    engine.trig(1);
  elseif d.type == 'note_off' then
    engine.trig(0);
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