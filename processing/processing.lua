-- processing chamber

local BGUtil = dofile(_path.code .. 'bitgraves/common/bgutil.lua')
local Hexagon = BGUtil.dofile_norns('common/hexagon.lua')

engine.name = 'Processing'
mid = nil

local triad = 0

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

  params:add_control('pShudder', 'pShudder', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('pShudder', function(x)
    engine.pShudder(x)
  end)
  
  params:add_control('detune', 'detune', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('detune', function(x)
    engine.detune(x)
  end)
  
  params:add_control('triad', 'triad', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('triad', function(x)
    triad = x
  end)
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end


function enc(nEnc, delta)

end

-- mapping from Akai MPD218 knobs to param handlers
local ccAkaiMapping = {
  [3] = 'triad',
  [9] = 'noteOffset',
  [12] = 'detune',
  [13] = 'pShudder',
  [14] = 'monitor',
  [15] = 'amp',
}

function key(...)
  BGUtil.setlist_key('processing/processing', ...)
end

local ccHandlers = {
  ['triad'] = function(val)
      params:set('triad', val)
      return 'triad ' .. tostring(val)
    end,
  ['noteOffset'] = function(val)
      params:set('noteOffset', val)
      local printVal = math.floor(util.linlin(0, 1, -4, -24, val))
      return 'pad offset ' .. tostring(printVal)
    end,
  ['detune'] = function(val)
      params:set('detune', val)
      return 'detune ' .. tostring(val)
    end,
  ['pShudder'] = function(val)
      params:set('pShudder', val)
      return 'shudder ' .. tostring(val)
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
    if note == 3 then
      engine.monotonic(1)
    else
      engine.noteOn(note, 1)
      if triad > 0 and note > 12 then
        engine.noteOn(12, 0.8 * triad)
        if triad > 0.3 then
          engine.noteOn(7, 0.5 * triad)
        end
      end
    end
  elseif d.type == 'note_off' then
    local note = d.note - 36
    if note == 3 then
      engine.monotonic(0)
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