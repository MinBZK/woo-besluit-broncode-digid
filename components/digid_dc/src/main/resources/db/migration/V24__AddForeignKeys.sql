ALTER TABLE services
	ADD CONSTRAINT FK_ServiceStatus FOREIGN KEY (status_id) REFERENCES statuses(id),
	ADD CONSTRAINT FK_ServiceConnection FOREIGN KEY (connection_id) REFERENCES connections(id);

ALTER TABLE services_services
	ADD CONSTRAINT FK_ChildServiceService FOREIGN KEY (service_child_id) REFERENCES services(id),
	ADD CONSTRAINT FK_ParentServiceService FOREIGN KEY (service_parent_id) REFERENCES services(id),
	ADD CONSTRAINT FK_ServicesServiceStatus FOREIGN KEY (status_id) REFERENCES statuses(id);

ALTER TABLE connections
	ADD CONSTRAINT FK_ServiceOrganizationRole FOREIGN KEY (organization_role_id) REFERENCES organization_roles(id),
	ADD CONSTRAINT FK_ConnectionStatus FOREIGN KEY (status_id) REFERENCES statuses(id);

ALTER TABLE organization_roles ADD CONSTRAINT FK_OrganizationRoleStatus FOREIGN KEY (status_id) REFERENCES statuses(id);
ALTER TABLE organizations ADD CONSTRAINT FK_OrganizationStatus FOREIGN KEY (status_id) REFERENCES statuses(id);
ALTER TABLE keywords ADD CONSTRAINT FK_KeywordService FOREIGN KEY (service_id) REFERENCES services(id);

ALTER TABLE service_organization_roles
	ADD CONSTRAINT FK_ServiceOrganizationRoleOrganizationRole FOREIGN KEY (organization_role_id) REFERENCES organization_roles(id),
	ADD CONSTRAINT FK_ServiceOrganizationRoleService FOREIGN KEY (service_id) REFERENCES services(id),
	ADD CONSTRAINT FK_ServiceOrganizationRoleStatus FOREIGN KEY (status_id) REFERENCES statuses(id);
