-- lighthouse
-- requires Vowel quark

local BGUtil = include('bitgraves/common/bgutil')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = include('bitgraves/common/hexagon')

engine.name = 'Lighthouse'
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input
  audio.level_monitor(0) -- just reset for now...
  BGMidi.sendMapping("tanzbar", engine.addMidiMapping)

  BGUtil.addEngineControlParam(params, { id = "vowel" })
  BGUtil.addEngineControlParam(params, { id = "noise", max = 2 })
  BGUtil.addEngineControlParam(params, { id = "ana" })
  BGUtil.addEngineControlParam(params, { id = "basis", max = 16 })
  BGUtil.addEngineControlParam(params, { id = "kick" })
  BGUtil.addEngineControlParam(params, { id = "click", max = 2 })
  BGUtil.addEngineControlParam(params, { id = "clap" })
  BGUtil.addEngineControlParam(params, { id = "amp" })
  
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
    -- TODO: generalize
    local param = params:lookup_param('vowel')
    local maxval = param.controlspec.maxval
    print('maxval for vowel is: ' .. tostring(maxval))
    params:set('vowel', val)
    return 'vowel ' .. val
  end,
  ['noise'] = function(val)
    -- TODO: val will always be 0-1 (midi input)
    -- need to map to range of controlspec
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
