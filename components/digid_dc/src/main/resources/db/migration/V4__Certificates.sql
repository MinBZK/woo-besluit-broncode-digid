CREATE TABLE certificates (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  cached_certificate  TEXT NOT NULL,
  fingerprint VARCHAR(255) NOT NULL,
  distinguished_name VARCHAR(255) NOT NULL,
  organization_id INT,
  connection_id INT,
  service_id INT,
  service_definition_id INT,
  active_from DATETIME NOT NULL,
  active_until DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);

