-- processing chamber

local BGUtil = dofile(_path.code .. 'bitgraves/common/bgutil.lua')
local BGMidi = include('bitgraves/common/bgmidi')
local Hexagon = BGUtil.dofile_norns('common/hexagon.lua')

engine.name = 'Processing'
mid = nil

local triad = 0
local MPD218

function init()
  BGUtil.configureSystemStuff()

  BGUtil.addEngineControlParam(params, { id = "amp" })
  BGUtil.addEngineControlParam(params, {
    id = "noteOffset",
    max = 24,
    action = function(x) engine.noteOffset(x * -1) end, -- param won't work when max is -24 for some reason
  })
  BGUtil.addEngineControlParam(params, { id = "pShudder" })
  BGUtil.addEngineControlParam(params, { id = "detune" })
  BGUtil.addEngineControlParam(params, { id = "triad", action = function(x) triad = x end })
  
  params:add_control('monitor', 'monitor', controlspec.new(0, 1, 'lin', 0, 0, ''))
  params:set_action('monitor', function(x)
    audio.level_monitor(x)
  end)
  
  MPD218 = BGMidi.newInputMappingMPD218({
    [3] = 'triad',
    [9] = 'noteOffset',
    [12] = 'detune',
    [13] = 'pShudder',
    [14] = 'monitor',
    [15] = 'amp',
  })
  
  mid = midi.connect()
  mid.event = midiEvent
  redraw()
end

function key(...)
  BGUtil.setlist_key('processing/processing', ...)
end

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
    local handled, msg = BGMidi.handleCCMPD218(MPD218, params, d.cc, d.val)
    if handled then
      redraw(msg)
    end
  end
end

function redraw(msg)
  Hexagon:draw(MPD218, msg)
end