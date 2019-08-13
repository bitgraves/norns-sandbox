-- crystal

local BGUtil = dofile(_path.code .. 'bitgraves/common/bgutil.lua')
local Hexagon = BGUtil.dofile_norns('common/hexagon.lua')

engine.name = 'Crystal'
local mix = 0
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input

  params:add_control('amp', 'amp', controlspec.new(0, 1, 'lin', 0, 0.5, ''))
  params:set_action('amp', function(x)
    engine.amp(x)
  end)
  
  params:add_control('bend', 'bend', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('bend', function(x)
    engine.bend(x)
  end)
  
  params:add_control('glitch', 'glitch', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('glitch', function(x)
    engine.glitch(x)
  end)
  
  params:add_control('freqLpf', 'freqLpf', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('freqLpf', function(x)
    engine.freqLpf(util.linexp(0, 1, 20, 20000, x))
  end)
  
  params:add_control('monitor', 'monitor', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('monitor', function(x)
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

-- expose a couple params via enc for debugging
function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('monitor', delta)
  elseif nEnc == 3 then
    params:delta('bend', delta)
  end
end

function key(n, z)
  if n == 2 then
    if z == 1 then
      engine.noteOn(7)
    else
      engine.noteOff(7)
    end
  end
end

function key(...)
  BGUtil.setlist_key('crystal/crystal', ...)
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'bend',
  [9] = 'glitch',
  [13] = 'freqLpf',
  [14] = 'monitor',
  [15] = 'amp',
}

local ccHandlers = {
  ['bend'] = function(val)
      params:set('bend', val)
      return 'bend ' .. tostring(val)
    end,
  ['glitch'] = function(val)
      params:set('glitch', val)
      return 'glitch ' .. val
    end,
  ['freqLpf'] = function(val)
      params:set('freqLpf', val)
      return 'lpf ' .. val
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
    engine.noteOn(note)
  elseif d.type == 'note_off' then
    local note = d.note - 36
    engine.noteOff(note)
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