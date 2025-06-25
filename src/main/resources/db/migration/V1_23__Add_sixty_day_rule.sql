CREATE TABLE sixty_day_rules (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  person                       VARCHAR(255) NOT NULL,
  address                      VARCHAR(255) NOT NULL,
  count                        INT DEFAULT 0,
  last_drive_date              DATETIME NOT NULL
);

ALTER TABLE reports ADD COLUMN flagged_sixty_days BOOLEAN DEFAULT FALSE;