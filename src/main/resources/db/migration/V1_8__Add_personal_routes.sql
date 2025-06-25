CREATE TABLE personal_routes (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    description             VARCHAR(255) NOT NULL,
    person_id               BIGINT NOT NULL,

    FOREIGN KEY (person_id) REFERENCES persons(id)
);