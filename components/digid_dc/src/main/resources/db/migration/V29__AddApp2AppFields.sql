ALTER TABLE services ADD app_active BOOLEAN NOT NULL DEFAULT 0;
ALTER TABLE services ADD app_return_url VARCHAR(255);
