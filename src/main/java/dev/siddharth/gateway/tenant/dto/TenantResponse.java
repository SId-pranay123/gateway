package dev.siddharth.gateway.tenant.dto;

import dev.siddharth.gateway.tenant.TenantRateLimitConfig;

import java.time.Instant;

public record TenantResponse(
        String apiKey,
        String tenantId,
        Long capacity,
        Double refillRatePerSecond,
        Instant createdAt,
        Instant updatedAt) {

    public static TenantResponse from(TenantRateLimitConfig config) {
        return new TenantResponse(
                config.getApiKey(),
                config.getTenantId(),
                config.getCapacity(),
                config.getRefillRatePerSecond(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }
}
