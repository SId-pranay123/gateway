package dev.siddharth.gateway.ratelimit;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A single tenant's token bucket. Thread-safe via AtomicReference + compare-and-swap,
 * since the gateway is reactive/multi-threaded and multiple requests for the same
 * tenant can arrive concurrently.
 *
 * Algorithm: bucket holds up to `capacity` tokens. Tokens refill continuously at
 * `refillRatePerSecond`. Each allowed request consumes 1 token. If no tokens are
 * available, the request is rejected.
 */
public class TokenBucket {

    private final long capacity;
    private final double refillRatePerSecond;
    private final AtomicReference<State> state;

    public TokenBucket(long capacity, double refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.state = new AtomicReference<>(new State(capacity, Instant.now()));
    }

    /**
     * Attempts to consume one token. Returns true if allowed, false if rate-limited.
     */
    public boolean tryConsume() {
        while (true) {
            State current = state.get();
            State refilled = refill(current);

            if (refilled.tokens >= 1.0) {
                State next = new State(refilled.tokens - 1.0, refilled.lastRefill);
                if (state.compareAndSet(current, next)) {
                    return true;
                }
                // CAS failed due to concurrent update — retry
            } else {
                // Not enough tokens, but still persist the refilled state
                // so we don't lose the time-based refill progress.
                if (state.compareAndSet(current, refilled)) {
                    return false;
                }
                // CAS failed — retry
            }
        }
    }

    private State refill(State current) {
        Instant now = Instant.now();
        double secondsElapsed = (now.toEpochMilli() - current.lastRefill.toEpochMilli()) / 1000.0;
        if (secondsElapsed <= 0) {
            return current;
        }
        double newTokens = Math.min(capacity, current.tokens + secondsElapsed * refillRatePerSecond);
        return new State(newTokens, now);
    }

    private record State(double tokens, Instant lastRefill) {
    }
}
