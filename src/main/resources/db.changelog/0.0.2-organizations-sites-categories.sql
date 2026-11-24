--liquibase formatted sql

--changeset enterprise:0.0.2-organizations-sites-assets-categories
--comment: Schema for organizations, sites, assets, user_organizations, categories, and subcategories

CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS sites (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(50) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_site_code_org UNIQUE (organization_id, code)
);

CREATE TABLE IF NOT EXISTS assets (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    site_id BIGINT REFERENCES sites(id) ON DELETE SET NULL,
    asset_tag VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_asset_tag_org UNIQUE (organization_id, asset_tag)
);

CREATE TABLE IF NOT EXISTS user_organizations (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, organization_id)
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS subcategories (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uq_subcategory_code_cat UNIQUE (category_id, code)
);

-- Alter tickets table to link with organizations, sites, assets, channel, and subcategories
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS organization_id BIGINT REFERENCES organizations(id);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS site_id BIGINT REFERENCES sites(id);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS asset_id BIGINT REFERENCES assets(id);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS channel VARCHAR(50) DEFAULT 'WEB' NOT NULL;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES categories(id);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS subcategory_id BIGINT REFERENCES subcategories(id);

-- Seed default Organization, Site, and Asset
INSERT INTO organizations (name, code) VALUES
    ('Corporate HQ', 'ORG-CORP'),
    ('Acme Global', 'ORG-ACME')
ON CONFLICT (code) DO NOTHING;

INSERT INTO sites (organization_id, name, code, address, city) VALUES
    (1, 'Headquarters Main Campus', 'SITE-HQ-01', '100 Enterprise Way', 'New York'),
    (2, 'Acme Factory Site A', 'SITE-ACME-01', '50 Industrial Blvd', 'Chicago')
ON CONFLICT (organization_id, code) DO NOTHING;

INSERT INTO assets (organization_id, site_id, asset_tag, name) VALUES
    (1, 1, 'AST-1001', 'Core Switch CISCO-9300'),
    (1, 1, 'AST-1002', 'Corporate Laptop DELL-7420'),
    (2, 2, 'AST-2001', 'Factory Workstation HP-Z2')
ON CONFLICT (organization_id, asset_tag) DO NOTHING;

-- Seed default Categories and Subcategories (FR-005)
INSERT INTO categories (name, code, description) VALUES
    ('Network & Connectivity', 'CAT-NET', 'Issues relating to VPN, Wi-Fi, switches, routers'),
    ('Hardware & Equipment', 'CAT-HW', 'Issues relating to laptops, monitors, servers'),
    ('Software & Applications', 'CAT-SW', 'Issues relating to enterprise software and tools')
ON CONFLICT (code) DO NOTHING;

INSERT INTO subcategories (category_id, name, code, description) VALUES
    (1, 'VPN Access', 'SUB-VPN', 'VPN client errors and disconnections'),
    (1, 'Wi-Fi Network', 'SUB-WIFI', 'Office wireless access issues'),
    (2, 'Laptop Issue', 'SUB-LAPTOP', 'Laptop boot or physical failure'),
    (2, 'Monitor / Display', 'SUB-MONITOR', 'Display and docking station issues'),
    (3, 'Email Client', 'SUB-EMAIL', 'Outlook / Thunderbird issues'),
    (3, 'ERP Access', 'SUB-ERP', 'Enterprise ERP access issues')
ON CONFLICT (category_id, code) DO NOTHING;

--rollback ALTER TABLE tickets DROP COLUMN IF EXISTS subcategory_id; ALTER TABLE tickets DROP COLUMN IF EXISTS category_id; ALTER TABLE tickets DROP COLUMN IF EXISTS channel; ALTER TABLE tickets DROP COLUMN IF EXISTS asset_id; ALTER TABLE tickets DROP COLUMN IF EXISTS site_id; ALTER TABLE tickets DROP COLUMN IF EXISTS organization_id; DROP TABLE IF EXISTS subcategories; DROP TABLE IF EXISTS categories; DROP TABLE IF EXISTS user_organizations; DROP TABLE IF EXISTS assets; DROP TABLE IF EXISTS sites; DROP TABLE IF EXISTS organizations;
