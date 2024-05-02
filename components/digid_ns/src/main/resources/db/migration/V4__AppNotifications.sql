CREATE TABLE app_notifications (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  notification_id INT NOT NULL,
  app_notification_id VARCHAR(255) NOT NULL,
  date_sent DATETIME,
  notification_status VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX name ON app_notifications (id);
