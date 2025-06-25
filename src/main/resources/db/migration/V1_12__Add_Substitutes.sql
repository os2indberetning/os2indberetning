CREATE TABLE substitutes (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  start_date                   DATETIME NULL,
  end_date                     DATETIME NULL,
  substitute                   VARCHAR(255) NOT NULL,
  substitute_for               VARCHAR(255) NOT NULL,
  org_unit                     VARCHAR(255) NULL,
  substitute_exclusive_mode    BOOLEAN DEFAULT FALSE,
  created_by                   VARCHAR(255) NOT NULL
);