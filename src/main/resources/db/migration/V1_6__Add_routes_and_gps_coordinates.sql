CREATE TABLE routes (
    id              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    route_geometry  LONGTEXT
);

CREATE TABLE gps_coordinates (
    id              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    latitude        VARCHAR(255) NOT NULL,
    longitude       VARCHAR(255) NOT NULL,
    report_id       BIGINT NOT NULL,
    waypoint        BOOLEAN NOT NULL DEFAULT FALSE,
    address         VARCHAR(255)
);