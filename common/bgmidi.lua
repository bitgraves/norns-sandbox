-- bitgraves midi helpers

local BGMidi = {}

local midiOutDestinationMappings = {
  -- MFB tanzbar drum machine (the older one)
  tanzbar = {
    chan =  2,
    controlChan = 9,

    bd = -24,
    oh = -19,
    hh = -18,
    cl = -17,
    sd = -22,
    cp = -16,
    ltc = -15,
    mtc = -13,
    htc = -11,
  
    bd1_tune = 3,
    sd_tune = 11,
    mtc_tune = 21,
    mtc_decay = 22,
    hh_tune = 73,
    cp_filter = 18,
    cp_decay = 75,
  },
}

-- configure the SC engine midi out mapping.
-- sendMappingCommand must be a norns engine command with the OSC format "si".
-- the lua mapping must be a table with string keys and integer (cc) values.
function BGMidi.sendMapping(midiOutDestination, sendMappingCommand)
  if midiOutDestinationMappings[midiOutDestination] ~= nil then
    for k, v in pairs(midiOutDestinationMappings[midiOutDestination]) do
      sendMappingCommand(k, v)
    end
  else
    print("Undefined midi destination mapping: " .. tostring(midiOutDestination))
  end
end

function BGMidi.newInputMappingMPD218(mappings)
  local mapping = {
    ccMapping = {},
    ccHandlers = {},
  }
  for ccIndex, paramId in pairs(mappings) do
    BGMidi.addCCMPD218(mapping, ccIndex, paramId)
  end
  return mapping
end

function BGMidi.addCCMPD218(mapping, ccIndex, paramId)
  mapping.ccMapping[ccIndex] = paramId
  mapping.ccHandlers[paramId] = function(params, val)
    val = val / 127
    local param = params:lookup_param(paramId)
    local mappedVal = param.controlspec:map(val)
    params:set(paramId, mappedVal)
    return tostring(paramId) .. ' ' .. mappedVal
  end
end

function BGMidi.handleCCMPD218(mapping, params, cc, val)
  local paramId = mapping.ccMapping[cc]
  if paramId ~= nil and mapping.ccHandlers[paramId] ~= nil then
    local msg = mapping.ccHandlers[paramId](params, val)
    return true, msg
  end
  return false
end

return BGMidi
