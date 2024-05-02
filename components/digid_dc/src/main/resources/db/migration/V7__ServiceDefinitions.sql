CREATE TABLE service_definitions (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  uuid VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  minimum_reliability_level INT NOT NULL,
  permission_question VARCHAR(255),
  authorization_required BOOLEAN NOT NULL,
  encryption_id_type VARCHAR(255) NOT NULL,
  oin VARCHAR(255) NOT NULL,
  new_reliability_level INT,
  new_reliability_level_starting_date DATETIME,
  new_reliability_level_change_message VARCHAR(255),
  status_id INT NOT NULL
);
 CREATE UNIQUE INDEX name ON service_definitions (uuid);
