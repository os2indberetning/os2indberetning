CREATE TABLE rate_types (
    id          BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(255)
);

CREATE TABLE rates (
    id                  BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    rate_per_km         INT NOT NULL,
    active_year         INT NOT NULL,
    rate_type_id        BIGINT NOT NULL,
    pay_type            INT,
    sequential_number   INT,

    FOREIGN KEY (rate_type_id) REFERENCES rate_types(id)
);