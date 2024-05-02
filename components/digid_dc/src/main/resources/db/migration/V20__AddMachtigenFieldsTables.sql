CREATE TABLE service_organization_roles (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organization_role_id INT NOT NULL,
  service_id INT NOT NULL,
  status_id INT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE keywords (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  service_id INT NOT NULL,
  text VARCHAR(255)
);

CREATE TABLE services_services (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  service_child_id INT NOT NULL,
  service_parent_id INT NOT NULL,
  type VARCHAR(255) NOT NULL,
  status_id INT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

RENAME TABLE service_definitions TO services;

ALTER TABLE certificates
DROP COLUMN service_id,
CHANGE COLUMN service_definition_id service_id INT;

ALTER TABLE services
ADD COLUMN digid BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN machtigen BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN legacy_machtigen_id INT,
ADD COLUMN position SMALLINT(6) DEFAULT 0,
ADD COLUMN authorization_type VARCHAR(255),
ADD COLUMN duration_authorization INT,
ADD COLUMN description VARCHAR(300),
ADD COLUMN explanation VARCHAR(2000),
ADD COLUMN status_id INT,
DROP COLUMN active;




