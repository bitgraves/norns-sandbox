-- hexagon
-- renders Akai MPD218 state

local Hexagon = {
  center = { x = 64, y = 32 },
  r = 30,
  incAngle = (math.pi * 2) / 6,
  ccIndex = { 13, 15, 14, 12, 3, 9 }, -- Akai MPD218
}

-- renders Hexagon, which is not really a hexagon.
-- @param MPD218Mapping a table returned from BGMidi.newInputMappingMPD218()
function Hexagon:draw(MPD218, msg)
  screen.clear()
  screen.level(8)

  local angle = self.incAngle * -0.5
  for region = 1, 6 do
    local nextAngle = angle + self.incAngle
    local paramId = MPD218.ccMapping[self.ccIndex[region] ]
    if paramId ~= nil and paramId ~= '' then
      local param = params:lookup_param(paramId)
      local val = params:get(paramId)
      local unmappedVal = param.controlspec:unmap(val)
      screen.move(self.center.x, self.center.y)
      screen.arc(self.center.x, self.center.y, self.r * unmappedVal, angle, nextAngle)
      screen.close()
      screen.stroke()
    end
    angle = nextAngle
  end
  
  if msg ~= nil then
    screen.move(0, 64 - 4)
    screen.text(msg)
  end
  
  screen.update()
end

return Hexagon