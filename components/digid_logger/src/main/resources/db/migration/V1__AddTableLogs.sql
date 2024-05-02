-- My digid logs
CREATE TABLE my_digid_logs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nr INT NOT NULL,
  ip_address VARCHAR(255) NOT NULL,
  session_id VARCHAR(255),
  transaction_id VARCHAR(255),
  data json NOT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_my_digid_log_nr ON my_digid_logs(nr);
CREATE INDEX ix_my_digid_log_session_id ON my_digid_logs(session_id);
CREATE INDEX ix_my_digid_log_transaction_id ON my_digid_logs(transaction_id);

CREATE TABLE my_digid_log_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  my_digid_log_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,

  CONSTRAINT FK_my_digid_log_account_log_id FOREIGN KEY (my_digid_log_id) REFERENCES my_digid_logs (id)
);
CREATE INDEX ix_my_digid_log_account_id ON my_digid_log_accounts(account_id);

CREATE TABLE my_digid_log_services (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  my_digid_log_id BIGINT NOT NULL,
  service_id BIGINT NOT NULL,

  CONSTRAINT FK_my_digid_log_service_log_id FOREIGN KEY (my_digid_log_id) REFERENCES my_digid_logs (id)
);
CREATE INDEX ix_my_digid_log_service_id ON my_digid_log_services(service_id);

-- transaction logs
CREATE TABLE transaction_logs (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  nr INT NOT NULL,
  ip_address VARCHAR(255) NOT NULL,
  session_id VARCHAR(255),
  transaction_id VARCHAR(255),
  data json NOT NULL,
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_t_log_nr ON transaction_logs(nr);
CREATE INDEX ix_t_log_session_id ON transaction_logs(session_id);
CREATE INDEX ix_t_log_transaction_id ON transaction_logs(transaction_id);

CREATE TABLE transaction_log_accounts (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  transaction_log_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  CONSTRAINT FK_transaction_log_account_log_id FOREIGN KEY (transaction_log_id) REFERENCES transaction_logs (id)
);
CREATE INDEX ix_t_log_account_id ON transaction_log_accounts(account_id);

CREATE TABLE transaction_log_managers (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  transaction_log_id BIGINT NOT NULL,
  manager_id BIGINT NOT NULL,
  CONSTRAINT FK_transaction_log_manager_log_id FOREIGN KEY (transaction_log_id) REFERENCES transaction_logs (id)
);
CREATE INDEX ix_t_log_manager_id ON transaction_log_managers(manager_id);




