CREATE TABLE switches (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description VARCHAR(1024) NOT NULL,
  status VARCHAR(255) NOT NULL,
  pilot_group_id INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX name ON switches (id);

INSERT INTO `switches` (id, name, description, status, created_at, updated_at)
    VALUES  (1, "Koppeling met MCC Notificatie Service (MNS)", "Deze switch is standaard actief voor alle accounts en zorgt ervoor dat DigiD apps geregistreerd / gederegistreerd / geüpdatet kunnen worden voor pushnotificaties bij de MCC Notificatie Service (MNS) en er pushnotificaties naar DigiD apps verstuurd kunnen worden via de MNS", 1, NOW(), NOW())
    ;

