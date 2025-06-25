CREATE TABLE addresses (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    street_name             VARCHAR(255) NOT NULL,
    street_number           VARCHAR(255) NOT NULL,
    zip_code                INT NOT NULL,
    town                    VARCHAR(255) NOT NULL,
    longitude               VARCHAR(255) NOT NULL,
    latitude                VARCHAR(255) NOT NULL,
    description             VARCHAR(255),
    is_dirty                BOOLEAN,
    dirty_string            VARCHAR(255),
    type                    VARCHAR(255),
    person_id               BIGINT,
    personal_route_id       BIGINT,
    orgunit_id              BIGINT,
    standard_address        BOOLEAN DEFAULT FALSE,

    FOREIGN KEY (person_id) REFERENCES persons(id),
    FOREIGN KEY (personal_route_id) REFERENCES personal_routes(id),
    FOREIGN KEY (orgunit_id) REFERENCES orgunits(id)
);