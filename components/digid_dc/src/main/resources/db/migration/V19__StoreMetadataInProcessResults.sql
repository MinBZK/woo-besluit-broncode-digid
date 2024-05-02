ALTER TABLE saml_metadata_process_results ADD metadata MEDIUMTEXT, ADD hash VARCHAR(255);
ALTER TABLE connections MODIFY saml_metadata MEDIUMTEXT;
