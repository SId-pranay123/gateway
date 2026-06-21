package dev.siddharth.gateway.tenant;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Wraps blocking JPA/Hibernate calls so they're safe to call from the
 * reactive gateway filter chain.
 *
 * Why this exists: Spring Data JPA is blocking (it talks to Postgres over
 * a synchronous JDBC connection). The gateway itself is reactive/non-blocking
 * (WebFlux), running on a small fixed pool of event-loop threads. If a
 * blocking JPA call ran directly on an event-loop thread, it would stall
 * that thread until the DB responds — starving other requests being handled
 * by the same thread. Schedulers.boundedElastic() runs the blocking call on
 * a separate thread pool meant for exactly this kind of blocking work, so
 * the event loop stays free.
 */
@Service
public class TenantConfigLookupService {

    private final TenantRateLimitConfigRepository repository;

    public TenantConfigLookupService(TenantRateLimitConfigRepository repository) {
        this.repository = repository;
    }

    public Mono<TenantRateLimitConfig> findByApiKey(String apiKey) {
        return Mono.fromCallable(() -> repository.findByApiKey(apiKey).orElse(null))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
