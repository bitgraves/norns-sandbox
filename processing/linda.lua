-- linda - WIP

local BGUtil = dofile(_path.code .. 'bitgraves/common/bgutil.lua')
local Hexagon = BGUtil.dofile_norns('common/hexagon.lua')

engine.name = 'Processing'
mid = nil

function init()
  audio:rev_off() -- no system reverb
  audio:pitch_off() -- no system pitch analysis
  audio:monitor_mono() -- expect only channel 1 input

  params:add_control('amp', 'amp', controlspec.new(0, 1, 'lin', 0, 0.5, ''))
  params:set_action('amp', function(x)
    engine.amp(x)
  end)
  
  params:add_control('noteOffset', 'noteOffset', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('noteOffset', function(x)
    engine.noteOffset(x)
  end)
  
  params:add_control('monitor', 'monitor', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('monitor', function(x)
    audio.level_monitor(x)
  end)
  
  params:add_control('filterCreep', 'filterCreep', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('filterCreep', function(x)
    engine.filterCreep(x)
  end)
  
  params:add_control('shudderDuration', 'shudderDuration', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('shudderDuration', function(x)
    engine.shudderDuration(x)
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
  end
end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [9] = 'noteOffset',
  [12] = 'filterCreep',
  [13] = 'shudderDuration',
  [14] = 'monitor',
  [15] = 'amp',
}

function key(...)
  BGUtil.setlist_key('processing/linda', ...)
end

local ccHandlers = {
  ['noteOffset'] = function(val)
      params:set('noteOffset', val)
      local printVal = math.floor(util.linlin(0, 1, -4, -24, val))
      return 'pad offset ' .. tostring(printVal)
    end,
  ['filterCreep'] = function(val)
      params:set('filterCreep', val)
      return 'filter creep ' .. tostring(val)
    end,
  ['shudderDuration'] = function(val)
      params:set('shudderDuration', val)
      return 'rhythm ' .. tostring(val)
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
  local d = midi.to_msg(data)
  if d.type == 'note_on' then
    local note = d.note - 36
    engine.noteOn(note)
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