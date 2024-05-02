CREATE TABLE notification_registrations (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  notification_id VARCHAR(255) NOT NULL,
  app_id VARCHAR(255) NOT NULL,
  account_id INT NOT NULL,
  device_name VARCHAR(255) NOT NULL,
  os_type INT NOT NULL,
  receive_notifications VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX name ON notification_registrations (app_id);