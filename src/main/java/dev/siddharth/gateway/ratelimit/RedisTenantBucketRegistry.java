package dev.siddharth.gateway.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Redis-backed replacement for TenantBucketRegistry. Same token bucket
 * algorithm, but state lives in Redis (via a Lua script) instead of an
 * in-memory map, so multiple gateway instances share the same counters.
 */
@Component
public class RedisTenantBucketRegistry {

    private static final long DEFAULT_CAPACITY = 5;
    private static final double DEFAULT_REFILL_RATE_PER_SECOND = 0.5;

    private final ReactiveStringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RedisTenantBucketRegistry(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("ratelimit.lua")));
        this.rateLimitScript.setResultType(Long.class);
    }

    public Mono<Boolean> tryConsume(String tenantId) {
        String key = "ratelimit:" + tenantId;
        double now = Instant.now().toEpochMilli() / 1000.0;

        List<String> keys = List.of(key);
        List<String> args = List.of(
                String.valueOf(DEFAULT_CAPACITY),
                String.valueOf(DEFAULT_REFILL_RATE_PER_SECOND),
                String.valueOf(now)
        );

        return redisTemplate.execute(rateLimitScript, keys, args)
                .next() // execute() returns a Flux; the script returns a single value
                .map(result -> result == 1L);
    }
}
