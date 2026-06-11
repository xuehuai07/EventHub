for i, key in ipairs(KEYS) do
    if redis.call('get', key) == ARGV[1] then
        redis.call('del', key)
    end
end
return 1
