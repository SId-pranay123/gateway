package dev.siddharth.gateway.tenant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateTenantRequest(
        @NotNull @Positive Long capacity,
        @NotNull @Positive Double refillRatePerSecond) {
}
