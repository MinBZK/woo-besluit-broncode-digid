CREATE TABLE document_status (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pseudonym varchar(255) NOT NULL,
    doc_type varchar(16) NOT NULL,
    state_source varchar(26) NOT NULL,
    sequence_nr varchar(16) NOT NULL,
    status varchar(26) NOT NULL,
    status_datetime datetime NOT NULL,
    status_mu varchar(12) NULL,
    status_mu_datetime datetime NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE UNIQUE INDEX pseudonym_doctype_seq_idx ON document_status (pseudonym, doc_type, sequence_nr);
