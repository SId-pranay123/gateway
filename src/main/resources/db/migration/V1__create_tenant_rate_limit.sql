CREATE TABLE tenant_rate_limit (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    tenant_id VARCHAR(255) NOT NULL UNIQUE,
    capacity BIGINT NOT NULL,
    refill_rate_per_second DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);