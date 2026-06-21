-- KEYS[1] = redis key for this tenant's bucket (e.g. "ratelimit:tenant-a")
-- ARGV[1] = capacity
-- ARGV[2] = refill rate per second
-- ARGV[3] = current timestamp (seconds, with decimal precision)

local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local bucket = redis.call("HMGET", KEYS[1], "tokens", "last_refill")
local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

if tokens == nil then
  tokens = capacity
  last_refill = now
end

local elapsed = now - last_refill
local refilled_tokens = math.min(capacity, tokens + elapsed * refill_rate)

local allowed = 0
if refilled_tokens >= 1 then
  allowed = 1
  refilled_tokens = refilled_tokens - 1
end

redis.call("HMSET", KEYS[1], "tokens", refilled_tokens, "last_refill", now)
redis.call("EXPIRE", KEYS[1], 3600)

return allowed