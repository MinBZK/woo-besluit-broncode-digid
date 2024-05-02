CREATE TABLE organizations (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  oin VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  description  VARCHAR(255),

  status_id INT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX name ON organizations (oin);

