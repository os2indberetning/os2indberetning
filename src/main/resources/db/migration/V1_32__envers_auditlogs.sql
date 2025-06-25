CREATE TABLE revinfo (
    id BIGINT                   NOT NULL PRIMARY KEY AUTO_INCREMENT,
    revtstmp                    BIGINT
);

CREATE TABLE employments_aud (
    id                              BIGINT NOT NULL,
    rev                             BIGINT NOT NULL,
    revtype                         TINYINT,

    employee_number                 VARCHAR(255),
    `position`                      VARCHAR(255),
    person_id                       BIGINT,
    leader                          BOOLEAN,
    start_date                      DATETIME,
    stop_date                       DATETIME,
    employment_type                 VARCHAR(255),
    orgunit_id                      BIGINT,
    extra_number                    INT,
    cost_center                     BIGINT,
    institution_code                VARCHAR(255),
    home_to_work_distance_override  DOUBLE,

    FOREIGN KEY fk_employments_aud_rev (rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_employments_aud(id, rev)
);


CREATE TABLE persons_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    cpr                         VARCHAR(10),
    first_name                  VARCHAR(255),
    last_name                   VARCHAR(255),
    email                       VARCHAR(255),
    active                      BOOLEAN,
    admin                       BOOLEAN,
    receive_email               BOOLEAN,
    receive_admin_mail          BOOLEAN,
    receive_approver_mail       BOOLEAN,
    receive_personal_mail       BOOLEAN,

    FOREIGN KEY fk_persons_aud_rev (rev) REFERENCES revinfo(id),
    PRIMARY KEY pk_persons_aud(id, rev)
);

CREATE TABLE app_logins_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    uuid                        VARCHAR(255),
    username                    VARCHAR(255),
    password                    VARCHAR(255),
    person_id                   BIGINT,

    FOREIGN KEY fk_app_logins_aud_rev(rev) REFERENCES revinfo (id),
    PRIMARY KEY pk_app_logins_aud(id, rev)
);

CREATE TABLE orgunits_aud (
    id                          BIGINT NOT NULL,
    rev                         BIGINT NOT NULL,
    revtype                     TINYINT,

    org_id                      VARCHAR(255),
    short_description           VARCHAR(255),
    long_description            VARCHAR(255),
    parent_id                   BIGINT,
    four_km_rule_allowed        BOOLEAN,
    calculation_type            VARCHAR(255),

    FOREIGN KEY fk_orgunits_aud_rev (rev) REFERENCES revinfo (id),
    PRIMARY KEY pk_orgunits_aud(id, rev)
);




