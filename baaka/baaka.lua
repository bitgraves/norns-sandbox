-- baaka

local BGUtil = dofile(_path.code .. 'bitgraves/common/bgutil.lua')
local Hexagon = BGUtil.dofile_norns('common/hexagon.lua')

engine.name = 'Baaka'
local mix = 0
local mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis

  params:add_control("amp", "amp", controlspec.new(0, 1, 'lin', 0, 0.5, ''))
  params:set_action("amp", function(x)
    engine.amp(x)
  end)
  
  params:add_control("mix", "mix", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("mix", function(x)
    engine.mix(x)
  end)
  
  params:add_control("noiseFreq", "noiseFreq", controlspec.new(0, 1, 'lin', 0, 1, ''))
  params:set_action("noiseFreq", function(x)
    engine.noiseFreq(util.linexp(0, 1, 1000, 18000, x))
  end)

  params:add_control("seqFreq", "seqFreq", controlspec.new(3, 51, 'lin', 0, 10, 'hz'))
  params:set_action("seqFreq", function(x)
    engine.seqFreq(x)
  end)
  
  params:add_control("oscFreq", "oscFreq", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("oscFreq", function(x)
    engine.oscFreq(util.linexp(0, 1, 110, 440, x))
  end)
  
  params:add_control("bend", "bend", controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action("bend", function(x)
    engine.bend(x)
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

-- expose a couple params via enc for debugging
function enc(nEnc, delta)
  if nEnc == 2 then
    params:delta('mix', delta)
  elseif nEnc == 3 then
    params:delta('noiseFreq', delta)
  end
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'bend',
  [9] = 'oscFreq',
  [12] = 'mix',
  [13] = 'noiseFreq',
  [14] = 'monitor',
  [15] = 'amp',
}

local ccHandlers = {
  ['bend'] = function(val)
      params:set('bend', val)
      return 'bend ' .. tostring(val)
    end,
  ['oscFreq'] = function(val)
      params:set('oscFreq', val)
      return 'mod freq ' .. val
    end,
  ['mix'] = function(val)
      mix = val
      params:set('mix', mix)
      return 'mix ' .. mix
    end,
  ['noiseFreq'] = function(val)
      params:set('noiseFreq', val)
      return 'noise freq ' .. val
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
      params:set('mix', 0)
      redraw('noise')
    else
      local newSeqFreq = (note * 3.0) + math.random(0, 3)
      params:set('mix', mix)
      params:set('seqFreq', newSeqFreq)
      redraw('seq ' .. newSeqFreq)
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