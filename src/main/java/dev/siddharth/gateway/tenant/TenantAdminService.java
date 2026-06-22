package dev.siddharth.gateway.tenant;

import dev.siddharth.gateway.tenant.dto.CreateTenantRequest;
import dev.siddharth.gateway.tenant.dto.TenantResponse;
import dev.siddharth.gateway.tenant.dto.UpdateTenantRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TenantAdminService {
    private final TenantRateLimitConfigRepository repository;

    public TenantAdminService(TenantRateLimitConfigRepository repository) {
        this.repository = repository;
    }

    public TenantResponse createConfig(CreateTenantRequest request) {
        if (repository.existsByApiKey(request.apiKey())) {
            throw new ResponseStatusException(CONFLICT, "Tenant config already exists for apiKey");
        }
        if (repository.existsByTenantId(request.tenantId())) {
            throw new ResponseStatusException(CONFLICT, "Tenant config already exists for tenantId");
        }

        TenantRateLimitConfig config = new TenantRateLimitConfig(
                request.apiKey(),
                request.tenantId(),
                request.capacity(),
                request.refillRatePerSecond());

        try {
            return TenantResponse.from(repository.save(config));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Tenant config already exists for apiKey or tenantId",
                    exception);
        }
    }

    public TenantResponse getCurrentConfig(String apiKey) {
        return TenantResponse.from(findConfig(apiKey));
    }

    public TenantResponse updateConfig(String apiKey, UpdateTenantRequest request) {
        TenantRateLimitConfig config = findConfig(apiKey);

        config.setCapacity(request.capacity());
        config.setRefillRatePerSecond(request.refillRatePerSecond());

        return TenantResponse.from(repository.save(config));
    }

    private TenantRateLimitConfig findConfig(String apiKey) {
        return repository.findByApiKey(apiKey)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tenant config not found"));
    }
}
