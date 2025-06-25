CREATE TABLE error_responses (
    id                  BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    response            LONGTEXT
);

CREATE TABLE error_logs (
    id                  BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    create_date         datetime NOT NULL,
    solved_date         datetime,
    assigned_to         BIGINT,
    solved_by           BIGINT,
    response_id         BIGINT NOT NULL,
    error_code          INT,
    error_text          VARCHAR(255),
    error_type          VARCHAR(255),
    comment             VARCHAR(255),
    email_sent          BOOLEAN DEFAULT FALSE,
    report_id           BIGINT,

    FOREIGN KEY (assigned_to) REFERENCES persons(id),
    FOREIGN KEY (solved_by) REFERENCES persons(id),
    FOREIGN KEY (response_id) REFERENCES error_responses(id),
    FOREIGN KEY (report_id) REFERENCES reports(id)
);