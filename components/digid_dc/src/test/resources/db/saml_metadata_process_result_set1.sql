TRUNCATE TABLE `saml_metadata_process_results`;
TRUNCATE TABLE `saml_metadata_process_errors`;

INSERT INTO `saml_metadata_process_results` VALUES (1,1,100,80,15,5,'2019-12-04 13:28:24','2019-12-06 08:17:08','','');
INSERT INTO `saml_metadata_process_results` VALUES (2,1,200,160,30,10,'2019-12-05 13:28:24','2019-12-05 08:17:08','','');
INSERT INTO `saml_metadata_process_results` VALUES (3,2,300,200,80,20,'2019-12-05 13:28:24','2019-12-05 08:17:08','','');

INSERT INTO `saml_metadata_process_errors` VALUES (1,1,"Large XML 1", "Error 1",'2019-12-05 13:28:24','2019-12-05 08:17:08');
INSERT INTO `saml_metadata_process_errors` VALUES (2,1,"Large XML 2", "Error 1",'2019-12-05 13:28:24','2019-12-05 08:17:08');
INSERT INTO `saml_metadata_process_errors` VALUES (3,2,"Large XML 3", "Error 1",'2019-12-05 13:28:24','2019-12-05 08:17:08');
INSERT INTO `saml_metadata_process_errors` VALUES (4,3,"Large XML 4", "Error 1",'2019-12-05 13:28:24','2019-12-05 08:17:08');
