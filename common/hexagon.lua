-- hexagon
-- renders Akai MPD218 state

local Hexagon = {
  center = { x = 64, y = 32 },
  r = 30,
  incAngle = (math.pi * 2) / 6,
  ccIndex = { 13, 15, 14, 12, 3, 9 }, -- Akai MPD218
}

-- renders Hexagon, which is not really a hexagon.
-- asks for values from params:get().
-- assumes they have range 0-1 for now.
-- @param ccParamMapping table mapping cc index to param name.
--        expects a key for each value in self.ccIndex, e.g. 3 -> 'bend'
function Hexagon:draw(msg, ccParamMapping)
  screen.clear()
  screen.level(8)

  local angle = self.incAngle * -0.5
  for region = 1, 6 do
    local nextAngle = angle + self.incAngle
    local paramName = ccParamMapping[self.ccIndex[region] ]
    if paramName ~= nil and paramName ~= '' then
      local val = params:get(paramName)
      screen.move(self.center.x, self.center.y)
      screen.arc(self.center.x, self.center.y, self.r * val, angle, nextAngle)
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