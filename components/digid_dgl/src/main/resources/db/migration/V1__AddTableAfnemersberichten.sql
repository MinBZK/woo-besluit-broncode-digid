CREATE TABLE afnemersberichten (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    error_code VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    bsn VARCHAR(255),
    a_nummer VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    onze_referentie VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
)