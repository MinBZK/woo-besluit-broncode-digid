CREATE TABLE connections (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  uuid VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  organization_role_id INT,
  version VARCHAR(255) NOT NULL,
  protocol_type VARCHAR(255) NOT NULL,
  saml_metadata TEXT,
  metadata_url VARCHAR(255),
  entity_id VARCHAR(255) NOT NULL,
  status_id INT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX name ON connections (uuid);

