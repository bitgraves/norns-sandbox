-- bitgraves util

local Script = require 'script'

BGUtil = {}

local setlist = {
  'forces/forces',
  'lighthouse/lighthouse',
  'marbles/marbles',
}

local function _findIndexInSetlist(fromPatchName)
  local currentIndex = 0
  for key, name in pairs(setlist) do
    if name == fromPatchName then
      currentIndex = key
      break
    end
  end
  return currentIndex
end

local function _loadScriptAtSetlistIndex(index)
  local name = setlist[index]
  Script.load(_path.code .. 'bitgraves/' .. name .. '.lua')
end

function BGUtil.dofile_norns(filepath)
  -- Dec 2020: seems no longer needed.
  -- use dofile because we can provide an absolute path.
  -- _path is defined in os.getenv('HOME') .. norns/lua/core/config.lua
  return dofile(_path.code .. 'bitgraves/' .. filepath)
end

--- map the norns 2 and 3 keys to setlist prev/next
function BGUtil.setlist_key(fromPatchName, n, isDown)
  if isDown == 1 then
    if n == 2 then
      BGUtil.setlist_prev(fromPatchName)
    elseif n == 3 then
      BGUtil.setlist_next(fromPatchName)
    end
  end
end

--- load the previous script in the setlist
function BGUtil.setlist_prev(fromPatchName)
  local currentIndex = _findIndexInSetlist(fromPatchName)
  if currentIndex > 0 then
    local prevIndex
    local length = #setlist
    if currentIndex == 1 then
      prevIndex = length
    else
      prevIndex = currentIndex - 1
    end
    _loadScriptAtSetlistIndex(prevIndex)
  end
end

--- load the next script in the setlist
function BGUtil.setlist_next(fromPatchName)
  local currentIndex = _findIndexInSetlist(fromPatchName)
  if currentIndex > 0 then
    local nextIndex
    local length = #setlist
    if currentIndex == length then
      nextIndex = 1
    else
      nextIndex = currentIndex + 1
    end
    _loadScriptAtSetlistIndex(nextIndex)
  end
end

return BGUtil