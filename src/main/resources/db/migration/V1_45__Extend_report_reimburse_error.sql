ALTER TABLE reports ADD COLUMN error_log_id BIGINT DEFAULT NULL;
ALTER TABLE reports ADD CONSTRAINT fk_reports_error_logs FOREIGN KEY (error_log_id) REFERENCES error_logs(id);
