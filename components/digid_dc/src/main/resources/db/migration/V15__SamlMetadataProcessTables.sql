CREATE TABLE saml_metadata_process_results (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  connection_id INT NOT NULL,
  total_processed INT NOT NULL,
  total_created   INT NOT NULL,
  total_updated  INT NOT NULL,
  total_errors  INT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE INDEX name ON saml_metadata_process_results (connection_id);

CREATE TABLE saml_metadata_process_errors (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  saml_metadata_process_result_id INT NOT NULL,
  service VARCHAR(8192) NOT NULL,
  error_reason  VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE INDEX name ON saml_metadata_process_errors (saml_metadata_process_result_id);
