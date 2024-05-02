CREATE TABLE configurations (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  value VARCHAR(255) NOT NULL,
  label  VARCHAR(255) NOT NULL,
  position INT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  default_value VARCHAR(255) NOT NULL
);
CREATE UNIQUE INDEX name ON configurations (name);

