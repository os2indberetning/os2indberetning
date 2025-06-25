CREATE TABLE auditlogs_details (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  content                      MEDIUMTEXT NOT NULL
);


CREATE TABLE auditlogs (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,

  -- metadata
  tts                          DATETIME NULL,

  -- performer
  performer_id                 BIGINT,
  performer_name               VARCHAR(255),

  -- structured log-data
  log_action                   VARCHAR(255) NOT NULL,

  -- full details
  auditlogs_details_id         BIGINT,

  -- abbreviated human-readable message
  message                      VARCHAR(500),

  CONSTRAINT fk_auditlogs_details FOREIGN KEY (auditlogs_details_id) REFERENCES auditlogs_details(id) ON DELETE CASCADE,
  INDEX(performer_id),
  INDEX(tts)
);