package dev.siddharth.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * Gateway filter that enforces a per-tenant token bucket rate limit,
 * backed by Redis so the limit is correct across multiple gateway instances.
 *
 * Tenant is identified via the "X-Tenant-ID" request header. If absent,
 * falls back to a "default" tenant bucket.
 */
@Component
public class TenantRateLimitGatewayFilterFactory
        extends AbstractGatewayFilterFactory<TenantRateLimitGatewayFilterFactory.Config> {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "default";

    private final RedisTenantBucketRegistry bucketRegistry;

    public TenantRateLimitGatewayFilterFactory(RedisTenantBucketRegistry bucketRegistry) {
        super(Config.class);
        this.bucketRegistry = bucketRegistry;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String tenantId = request.getHeaders().getFirst(TENANT_HEADER);
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = DEFAULT_TENANT;
            }

            return bucketRegistry.tryConsume(tenantId)
                    .flatMap(allowed -> {
                        if (allowed) {
                            return chain.filter(exchange);
                        } else {
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return exchange.getResponse().setComplete();
                        }
                    });
        };
    }

    public static class Config {
    }
}
