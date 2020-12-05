-- bounce

local BGUtil = include('bitgraves/common/bgutil')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Bounce'
local isOther = 0
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input

  params:add_control("amp", "amp", controlspec.new(0, 1, 'lin', 0, 1, ''))
  params:set_action("amp", function(x)
    engine.amp(x)
  end)

  params:add_control("bounce", "bounce", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("bounce", function(x)
    engine.bounce(x)
  end)

  params:add_control("drift", "drift", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("drift", function(x)
    engine.drift(x)
  end)

  params:add_control("lpf", "lpf", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("lpf", function(x)
    engine.lpf(x)
  end)

  params:add_control("hiTexAmp", "hiTexAmp", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("hiTexAmp", function(x)
    engine.hiTexAmp(x)
  end)
  
  params:add_control("monitor", "monitor", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("monitor", function(x)
    audio.level_monitor(x)
  end)
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

-- expose a couple params via enc for debugging
function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('bounce', delta)
  elseif nEnc == 3 then
    params:delta('lpf', delta)
  end
end

function key(...)
  BGUtil.setlist_key('bounce/bounce', ...)
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'hiTexAmp',
  [9] = 'bounce',
  [12] = 'drift',
  [13] = 'lpf',
  [14] = 'monitor',
  [15] = 'amp',
}

local ccHandlers = {
  ['hiTexAmp'] = function(val)
      params:set('hiTexAmp', val)
      return 'scratch amp ' .. val
    end,
  ['bounce'] = function(val)
    params:set('bounce', val)
    return 'bounce ' .. val
    end,
  ['drift'] = function(val)
      params:set('drift', val)
      return 'drift ' .. val
    end,
  ['lpf'] = function(val)
      params:set('lpf', val)
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
    if note == 0 then
      engine.other(1)
      isOther = 1
      redraw('')
    else
      engine.drift(note / 16)
      engine.driftTrig(1)
      redraw('sweep trig ' .. note / 16)
    end
  elseif d.type == 'note_off' then
    local note = d.note - 36
    if note == 0 then
      engine.other(0)
      isOther = 0
      redraw('')
    else
      engine.driftTrig(0)
      redraw('stop sweep')
    end
  elseif d.type == 'cc' then
    local handler = ccAkaiMapping[d.cc]
    if handler ~= nil and ccHandlers[handler] ~= nil then
      local msg = ccHandlers[handler](d.val / 127)
      redraw(msg)
    end
  end
end

function redraw(msg)
  if isOther == 1 then
    screen.clear()
    screen.level(12)
    screen.move(48, 32)
    screen.text('???????')
    screen.update()
  else
    Hexagon:draw(msg, ccAkaiMapping)
  end
end