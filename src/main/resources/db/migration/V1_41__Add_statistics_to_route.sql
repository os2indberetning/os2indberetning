CREATE TABLE statistics (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    function_name           VARCHAR(255),
    count                   BIGINT NOT NULL
);