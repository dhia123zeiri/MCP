-- =============================================================================
-- Doctor Office EAI — schema bootstrap
-- Runs once on first Postgres start (volume empty).
-- =============================================================================

-- ------------------------------------------------------------------
-- Reference data: countries (cannot be deleted per requirement #1)
-- ------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS country (
    code  VARCHAR(3)  PRIMARY KEY,        -- ISO-3166 alpha-2/3
    name  VARCHAR(80) NOT NULL UNIQUE
);

INSERT INTO country (code, name) VALUES
    ('PT', 'Portugal'),
    ('TN', 'Tunisia'),
    ('FR', 'France'),
    ('DE', 'Germany'),
    ('ES', 'Spain'),
    ('IT', 'Italy'),
    ('US', 'United States'),
    ('BR', 'Brazil')
ON CONFLICT (code) DO NOTHING;

-- ------------------------------------------------------------------
-- Pet types (items for sale) — table created by JPA; we just seed it
-- if it already exists. We use a DO block so this is idempotent.
-- ------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='pet_type') THEN
        INSERT INTO pet_type (species, breed, price) VALUES
            ('Dog', 'Husky',           120.00),
            ('Dog', 'Labrador',         95.00),
            ('Dog', 'Bulldog',         110.00),
            ('Cat', 'Persian',          80.00),
            ('Cat', 'Siamese',          75.00),
            ('Cat', 'Maine Coon',       90.00),
            ('Rabbit', 'Holland Lop',   45.00),
            ('Parrot', 'African Grey', 200.00)
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

-- ------------------------------------------------------------------
-- Result tables — written by Kafka Connect JDBC sink connectors.
-- ------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS res_revenue_per_item (
    pet_type_id BIGINT PRIMARY KEY,
    revenue     DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_expenses_per_item (
    pet_type_id BIGINT PRIMARY KEY,
    expenses    DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_profit_per_item (
    pet_type_id BIGINT PRIMARY KEY,
    profit      DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_totals (
    scope       VARCHAR(16) PRIMARY KEY,   -- 'revenue' | 'expenses' | 'profit'
    amount      DOUBLE PRECISION NOT NULL,
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_avg_per_appointment_by_item (
    pet_type_id BIGINT PRIMARY KEY,
    avg_amount  DOUBLE PRECISION NOT NULL,
    count       BIGINT NOT NULL,
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_avg_per_appointment_all (
    scope       VARCHAR(8) PRIMARY KEY,    -- always 'ALL'
    avg_amount  DOUBLE PRECISION NOT NULL,
    count       BIGINT NOT NULL,
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_top_profit_item (
    scope        VARCHAR(8) PRIMARY KEY,   -- always 'TOP'
    pet_type_id  BIGINT NOT NULL,
    profit       DOUBLE PRECISION NOT NULL,
    updated_at   TIMESTAMP DEFAULT NOW()
);

-- Windowed: separate tables per metric so window_key (petTypeId@start)
-- can be the natural PK without conflict between revenue/expenses/profit sinks.
CREATE TABLE IF NOT EXISTS res_windowed_revenue (
    window_key   VARCHAR(64) PRIMARY KEY,
    metric       VARCHAR(16) NOT NULL DEFAULT 'revenue',
    amount       DOUBLE PRECISION NOT NULL,
    updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_windowed_expenses (
    window_key   VARCHAR(64) PRIMARY KEY,
    metric       VARCHAR(16) NOT NULL DEFAULT 'expenses',
    amount       DOUBLE PRECISION NOT NULL,
    updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_windowed_profit (
    window_key   VARCHAR(64) PRIMARY KEY,
    metric       VARCHAR(16) NOT NULL DEFAULT 'profit',
    amount       DOUBLE PRECISION NOT NULL,
    updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS res_top_country_per_item (
    pet_type_id    BIGINT PRIMARY KEY,
    country_code   VARCHAR(3) NOT NULL,
    sales_amount   DOUBLE PRECISION NOT NULL,
    updated_at     TIMESTAMP DEFAULT NOW()
);
