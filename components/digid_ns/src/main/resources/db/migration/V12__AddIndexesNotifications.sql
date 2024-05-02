ALTER TABLE notification_registrations ADD INDEX created_at (created_at);
ALTER TABLE notification_registrations ADD INDEX updated_at (updated_at);

ALTER TABLE notifications ADD INDEX created_at (created_at);
ALTER TABLE notifications ADD INDEX updated_at (updated_at);

ALTER TABLE app_notifications ADD INDEX created_at (created_at);
ALTER TABLE app_notifications ADD INDEX updated_at (updated_at);