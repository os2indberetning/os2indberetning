CREATE TABLE personal_route_address_mapping (
    id                  BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    waypoint            BOOLEAN NOT NULL DEFAULT false,
    start_point         BOOLEAN NOT NULL DEFAULT false,
    end_point           BOOLEAN NOT NULL DEFAULT false,
    point_number        INTEGER NOT NULL,
    personal_route_id   BIGINT NOT NULL,
    address_id          BIGINT NOT NULL,

    CONSTRAINT FK_PERSONAL_ROUTE_ADDRESS_MAPPING_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id),
    CONSTRAINT FK_PERSONAL_ROUTE_ADDRESS_MAPPING_ON_PERSONAL_ROUTE FOREIGN KEY (personal_route_id) REFERENCES personal_routes (id)
);

ALTER TABLE personal_routes ADD COLUMN route_geometry   LONGTEXT;
ALTER TABLE personal_routes ADD COLUMN start_address    VARCHAR(255) NOT NULL;
ALTER TABLE personal_routes ADD COLUMN end_address      VARCHAR(255) NOT NULL;
