package dev.siddharth.gateway.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRateLimitConfigRepository extends JpaRepository<TenantRateLimitConfig, Long> {
    Optional<TenantRateLimitConfig> findByApiKey(String apiKey);
}
