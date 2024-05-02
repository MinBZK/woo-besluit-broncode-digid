UPDATE organization_roles set type = "AFNEMER" where type ="af";
UPDATE organization_roles set type = "ZELFSTANDIGE_AANSLUITHOUDER" where type ="za";
UPDATE organization_roles set type = "LEVERANCIER_CLUSTERAANSLUITING" where type ="lc";
UPDATE organization_roles set type = "LEVERANCIER_ROUTERINGSVOORZIENING" where type ="lr";

UPDATE services set authorization_type = "BURGER_EN_ORGANISATIE" where authorization_type ="Burger en organisatie";
UPDATE services set authorization_type = "BURGER" where authorization_type ="Burger";
UPDATE services set authorization_type = "ORGANISATIE" where authorization_type ="Organisatie";
UPDATE services set authorization_type = "NIET" where authorization_type ="Niet";

UPDATE services set encryption_id_type = "LEGACY_BSN" where authorization_type ="Legacy BSN";
UPDATE services set encryption_id_type = "BSN" where authorization_type ="Bsn";
UPDATE services set encryption_id_type = "PSEUDONIEM" where authorization_type ="Pseudoniem";
