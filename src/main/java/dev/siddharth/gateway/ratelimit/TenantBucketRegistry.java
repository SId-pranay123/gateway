package dev.siddharth.gateway.ratelimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds one TokenBucket per tenant, in memory.
 *
 * NOTE: this is intentionally in-memory only (Step 3). It will NOT stay correct
 * once we run multiple gateway instances — each instance will have its own
 * separate buckets, so a tenant could exceed their real limit by hitting
 * different instances. That's exactly the problem Step 4 (Redis-backed state)
 * solves. Keeping this in-memory version isolates "is the algorithm correct"
 * from "is the distributed state correct" as two separate, separately-tested
 * concerns.
 */
@Component
public class TenantBucketRegistry {

    // TODO (Step 5): replace these hardcoded defaults with per-tenant config
    // loaded from Postgres.
    private static final long DEFAULT_CAPACITY = 5;
    private static final double DEFAULT_REFILL_RATE_PER_SECOND = 0.5; // 1 token per 2s

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String tenantId) {
        TokenBucket bucket = buckets.computeIfAbsent(
                tenantId,
                id -> new TokenBucket(DEFAULT_CAPACITY, DEFAULT_REFILL_RATE_PER_SECOND)
        );
        return bucket.tryConsume();
    }
}
