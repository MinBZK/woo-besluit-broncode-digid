CREATE TABLE messages (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  message_type VARCHAR(255) NOT NULL,
  title_english VARCHAR(255) NOT NULL,
  content_english VARCHAR(255) NOT NULL,
  title_dutch VARCHAR(255) NOT NULL,
  content_dutch VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
);
CREATE UNIQUE INDEX name ON messages (id);

