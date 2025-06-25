CREATE TABLE app_logins (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    uuid                    VARCHAR(36),
    username                VARCHAR(255) UNIQUE NOT NULL,
    password                VARCHAR(255) NOT NULL,
    person_id               BIGINT NOT NULL,

    FOREIGN KEY (person_id) REFERENCES persons(id)
);