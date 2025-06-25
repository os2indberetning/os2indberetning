CREATE TABLE orgunits (
    id                  BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    org_id              VARCHAR(255) NULL,
    short_description   VARCHAR(255) NULL,
    long_description    VARCHAR(255) NULL,
    parent_id           BIGINT NULL,
    address_id          BIGINT,

    FOREIGN KEY fk_org_unit_parent (parent_id) REFERENCES orgunits(id)
);

CREATE TABLE employments (
    `id`                 BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `employment_id`      VARCHAR(255) NULL,
    `position`           VARCHAR(255) NULL,
    `person_id`          BIGINT NULL,
    `leader`             BOOLEAN NOT NULL,
    `start_date`         datetime NULL,
    `stop_date`          datetime NULL,
    `employment_type`    VARCHAR(255) NULL,
    `orgunit_id`         BIGINT NOT NULL,
    `extra_number`       INT NOT NULL,
    `cost_center`        BIGINT NOT NULL,
    `institution_code`   VARCHAR(255) NULL,

    FOREIGN KEY FK_EMPLOYMENT_ON_ORG_UNIT (orgunit_id) REFERENCES orgunits(id),
    FOREIGN KEY FK_EMPLOYMENT_ON_PERSON (person_id) REFERENCES persons(id)
);

ALTER TABLE persons ADD COLUMN admin BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE persons ADD COLUMN receive_email BOOLEAN NOT NULL DEFAULT false;
