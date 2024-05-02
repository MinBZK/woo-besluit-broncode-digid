ALTER TABLE services MODIFY connection_id INT;
ALTER TABLE services DROP FOREIGN KEY FK_ServiceConnection;
ALTER TABLE services MODIFY encryption_id_type varchar(255);
ALTER TABLE services MODIFY legacy_service_id INT;
ALTER TABLE services MODIFY entity_id VARCHAR(255);
ALTER TABLE organizations DROP COLUMN legacy_machtigen_id;
