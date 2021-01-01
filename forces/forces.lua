-- forces

local BGUtil = include('bitgraves/common/bgutil')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Forces'
mid = nil

local indexOffset = 2

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input
  audio.level_monitor(0) -- just reset for now...

  params:add_control("peaks", "peaks", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("peaks", function(x)
    engine.peaks(util.linlin(0, 1, 32, 1, x))
  end)
  
  params:add_control("noise", "noise", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("noise", function(x)
    engine.noise(util.linlin(0, 1, 0, 0.1, x))
  end)
  
  params:add_control("ana", "ana", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("ana", function(x)
    engine.ana(x)
  end)

  params:add_control("freqbase", "freqbase", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("freqbase", function(x)
    engine.freqbase(util.linlin(0, 1, 1, 2, x))
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
  BGUtil.setlist_key('forces/forces', ...)
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'peaks',
  [9] = 'noise',
  [12] = 'ana',
  [13] = 'freqbase',
  [14] = 'monitor',
  [15] = 'amp',
}

local ccHandlers = {
  ['peaks'] = function(val)
    params:set('peaks', val)
    return 'peaks ' .. val
  end,
  ['noise'] = function(val)
    params:set('noise', val)
    return 'noise ' .. val
  end,
  ['ana'] = function(val)
    params:set('ana', val)
    return 'ana ' .. val
  end,
  ['freqbase'] = function(val)
    params:set('freqbase', val)
    return 'freq base ' .. val
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
    if index <= 11 then
      engine.filterNoteOn(index)
      redraw('freq mult ' .. index)
    elseif index < 16 then
      local param = index - 12
      local seqDur = (0.035 + (param * 0.06)) * (0.85 + math.random() * 0.03);
      engine.seqDur(seqDur)
      redraw('seq dur ' .. seqDur)
    else
      engine.contraNoteOn(index - 16)
      redraw('note')
    end
    -- engine.noteOn(index + indexOffset)
  elseif d.type == 'note_off' then
    local index = d.note - 36
    if index < 11 then
      engine.filterNoteOff(0)
    elseif index >= 16 then
      engine.contraNoteOff(0)
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
  Hexagon:draw(msg, ccAkaiMapping)
end
