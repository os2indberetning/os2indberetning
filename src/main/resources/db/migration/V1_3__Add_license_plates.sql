CREATE TABLE license_plates (
  id                           BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
  registration_number          VARCHAR(255) NOT NULL,
  description                  VARCHAR(255),
  person_id                    BIGINT NOT NULL,
  prime                        BOOLEAN NOT NULL DEFAULT FALSE,

  FOREIGN KEY (person_id) REFERENCES persons(id)
);