CREATE TABLE api_access (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_key     VARCHAR(255) NOT NULL,
    type        VARCHAR(255) NOT NULL,
    disabled    BOOLEAN NOT NULL DEFAULT FALSE
);