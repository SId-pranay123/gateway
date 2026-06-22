package dev.siddharth.gateway.tenant;

import dev.siddharth.gateway.tenant.dto.CreateTenantRequest;
import dev.siddharth.gateway.tenant.dto.TenantResponse;
import dev.siddharth.gateway.tenant.dto.UpdateTenantRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;


@RestController
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    public TenantAdminController(TenantAdminService tenantAdminService){
        this.tenantAdminService = tenantAdminService;
    }

    @PostMapping("/admin/tenants")
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse createConfig(@Valid @RequestBody CreateTenantRequest request) {
        return this.tenantAdminService.createConfig(request);
    }

    @GetMapping("/admin/tenants/{apiKey}")
    public TenantResponse getCurrentConfig(@PathVariable String apiKey) {
        return this.tenantAdminService.getCurrentConfig(apiKey);
    }

    @PutMapping("/admin/tenants/{apiKey}")
    public TenantResponse updateConfig(@PathVariable String apiKey, @Valid @RequestBody UpdateTenantRequest request) {
        return this.tenantAdminService.updateConfig(apiKey, request);
    }

}
