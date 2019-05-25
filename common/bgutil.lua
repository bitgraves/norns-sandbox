-- bitgraves util

BGUtil = {}

function BGUtil.dofile_norns(filepath)
  -- use dofile because we can provide an absolute path.
  -- _path is defined in os.getenv('HOME') .. norns/lua/core/config.lua
  return dofile(_path.code .. 'bitgraves/' .. filepath)
end

return BGUtil