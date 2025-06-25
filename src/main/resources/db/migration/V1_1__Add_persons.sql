CREATE TABLE persons (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  cpr                          VARCHAR(10) NOT NULL,
  first_name                   VARCHAR(255) NOT NULL,
  last_name                    VARCHAR(255) NOT NULL,
  email                        VARCHAR(255),
  active                       BOOLEAN NOT NULL DEFAULT TRUE
);