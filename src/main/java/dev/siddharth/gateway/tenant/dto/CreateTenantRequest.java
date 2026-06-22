package dev.siddharth.gateway.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTenantRequest(
        @NotBlank String apiKey,
        @NotBlank String tenantId,
        @NotNull @Positive Long capacity,
        @NotNull @Positive Double refillRatePerSecond) {
}
