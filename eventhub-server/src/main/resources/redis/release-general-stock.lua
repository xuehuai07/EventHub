local members = redis.call('zrange', KEYS[1], 0, -1)
for i, member in ipairs(members) do
    if string.sub(member, 1, string.len(ARGV[1]) + 1) == ARGV[1] .. ':' then
        redis.call('zrem', KEYS[1], member)
    end
end
return 1
