package dev.siddharth.gateway.ratelimit;

import dev.siddharth.gateway.tenant.TenantConfigLookupService;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway filter that enforces a per-tenant token bucket rate limit,
 * backed by Redis so the limit is correct across multiple gateway instances.
 *
 * Tenant config is selected by API key so clients do not choose their own
 * tenant ID or rate-limit settings.
 */
@Component
public class TenantRateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<TenantRateLimitGatewayFilterFactory.Config> {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final RedisTenantBucketRegistry bucketRegistry;
    private final TenantConfigLookupService tenantConfigLookupService;

    private final MeterRegistry meterRegistry;

    public TenantRateLimitGatewayFilterFactory(
            RedisTenantBucketRegistry bucketRegistry,
            TenantConfigLookupService tenantConfigLookupService,
            MeterRegistry meterRegistry) {
        super(Config.class);
        this.bucketRegistry = bucketRegistry;
        this.tenantConfigLookupService = tenantConfigLookupService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String apiKey = request.getHeaders().getFirst(API_KEY_HEADER);
            if (apiKey == null || apiKey.isBlank()) {
                return complete(exchange, HttpStatus.UNAUTHORIZED);
            }

            final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TenantRateLimitGatewayFilterFactory.class);

            return tenantConfigLookupService.findByApiKey(apiKey)
                    .switchIfEmpty(Mono.defer(() -> complete(exchange, HttpStatus.UNAUTHORIZED).then(Mono.empty())))
                    .flatMap(tenantConfig -> bucketRegistry.tryConsume(
                            tenantConfig.getTenantId(),
                            tenantConfig.getCapacity(),
                            tenantConfig.getRefillRatePerSecond())
                    .flatMap(allowed -> {
                        if (allowed) {
                            meterRegistry.counter("gateway.requests",
                                "tenantId", tenantConfig.getTenantId(),
                                "result", "allowed").increment();
                            log.info("ALLOWED tenantId={} apiKey={}", tenantConfig.getTenantId(), apiKey);
                            return chain.filter(exchange);
                        }
                        meterRegistry.counter("gateway.requests",
                            "tenantId", tenantConfig.getTenantId(),
                            "result", "rate_limited").increment();
                        log.warn("RATE_LIMITED tenantId={} apiKey={}", tenantConfig.getTenantId(), apiKey);
                        return complete(exchange, HttpStatus.TOO_MANY_REQUESTS);
                    }));
        };
    }

    private Mono<Void> complete(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}
