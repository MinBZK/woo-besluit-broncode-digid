CREATE TABLE service_service_definitions (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  service_id INT NOT NULL,
  service_definition_id INT NOT NULL,
  status_id INT NOT NULL
);
