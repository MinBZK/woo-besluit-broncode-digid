TRUNCATE TABLE connections;
TRUNCATE TABLE certificates;
TRUNCATE TABLE service_definitions;
ALTER TABLE connections DROP uuid;
ALTER TABLE certificates DROP organization_id;
DROP TABLE services;
DROP TABLE service_service_definitions;
ALTER TABLE service_definitions DROP uuid, DROP oin;
ALTER TABLE service_definitions CHANGE COLUMN status_id active BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE service_definitions ADD entity_id VARCHAR(255) NOT NULL, ADD connection_id INT NOT NULL, ADD legacy_service_id INT NOT NULL, ADD service_uuid VARCHAR(255);


