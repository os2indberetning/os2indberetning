CREATE TABLE error_comments (
    id                  BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    comment             LONGTEXT
);

ALTER TABLE error_logs DROP COLUMN comment;
ALTER TABLE error_logs ADD COLUMN comment_id BIGINT;
ALTER TABLE error_logs ADD CONSTRAINT fk_error_comments_error_logs FOREIGN KEY (comment_id) REFERENCES error_comments(id);