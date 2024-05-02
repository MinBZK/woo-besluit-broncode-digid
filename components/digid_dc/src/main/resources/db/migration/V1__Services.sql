CREATE TABLE services (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  uuid VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(255) NOT NULL,
  login_url VARCHAR(255) NOT NULL,
  redirect_url VARCHAR(255) NOT NULL,
  connection_id INT NOT NULL,
  organization_id INT NOT NULL,
  legacy_id INT NOT NULL,
  redirect_domain VARCHAR(255) NOT NULL,
  error_return_url VARCHAR(255) NOT NULL,
  status_id INT NOT NULL,
  sso_domain VARCHAR(255) NOT NULL,
  sso_logout_url VARCHAR(255) NOT NULL,
  sso_status BOOLEAN NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX name ON services (uuid);

