for i, key in ipairs(KEYS) do
    if redis.call('exists', key) == 1 then
        return 0
    end
end
for i, key in ipairs(KEYS) do
    redis.call('psetex', key, ARGV[2], ARGV[1])
end
return 1
