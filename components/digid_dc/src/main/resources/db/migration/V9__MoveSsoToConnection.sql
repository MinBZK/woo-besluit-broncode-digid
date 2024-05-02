ALTER TABLE services
	DROP sso_domain, DROP sso_logout_url, DROP sso_status;

ALTER TABLE connections
	ADD sso_domain VARCHAR(255),
	ADD sso_status BOOLEAN;
