CREATE TABLE organization_roles (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  organization_id INT NOT NULL,
  type VARCHAR(255) NOT NULL,
  status_id INT NOT NULL
);
