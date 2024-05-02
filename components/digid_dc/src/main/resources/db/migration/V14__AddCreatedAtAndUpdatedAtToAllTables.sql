ALTER TABLE statuses ADD created_at DATETIME NOT NULL default NOW();
ALTER TABLE statuses ADD updated_at DATETIME NOT NULL default NOW();
ALTER TABLE organization_roles ADD created_at DATETIME NOT NULL default NOW();
ALTER TABLE organization_roles ADD updated_at DATETIME NOT NULL default NOW();
ALTER TABLE service_definitions ADD created_at DATETIME NOT NULL default NOW();
ALTER TABLE service_definitions ADD updated_at DATETIME NOT NULL default NOW();
ALTER TABLE service_service_definitions ADD created_at DATETIME NOT NULL default NOW();
ALTER TABLE service_service_definitions ADD updated_at DATETIME NOT NULL default NOW();
