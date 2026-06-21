package dev.siddharth.gateway.tenant;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenant_rate_limit")
public class TenantRateLimitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private String tenantId;

    @Column(nullable = false)
    private Long capacity;

    @Column(name = "refill_rate_per_second", nullable = false)
    private Double refillRatePerSecond;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected TenantRateLimitConfig() {
        // required by JPA
    }

    public TenantRateLimitConfig(String apiKey, String tenantId, Long capacity, Double refillRatePerSecond) {
        this.apiKey = apiKey;
        this.tenantId = tenantId;
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getCapacity() {
        return capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
        this.updatedAt = Instant.now();
    }

    public Double getRefillRatePerSecond() {
        return refillRatePerSecond;
    }

    public void setRefillRatePerSecond(Double refillRatePerSecond) {
        this.refillRatePerSecond = refillRatePerSecond;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
