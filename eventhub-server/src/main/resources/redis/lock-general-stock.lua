local now = tonumber(ARGV[1])
local expiresAt = tonumber(ARGV[2])
local availableStock = tonumber(ARGV[3])
local quantity = tonumber(ARGV[4])
local lockNo = ARGV[5]
redis.call('zremrangebyscore', KEYS[1], '-inf', now)
local locked = redis.call('zcard', KEYS[1])
if availableStock - locked < quantity then
    return 0
end
for i = 1, quantity do
    redis.call('zadd', KEYS[1], expiresAt, lockNo .. ':' .. i)
end
redis.call('pexpire', KEYS[1], math.max(expiresAt - now, 1000))
return 1
