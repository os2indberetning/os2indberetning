ALTER TABLE addresses ADD COLUMN deviating_address BIGINT;
ALTER TABLE addresses ADD FOREIGN KEY (deviating_address) REFERENCES addresses(id);

ALTER TABLE addresses ADD COLUMN home_to_work_distance_override_deviation DOUBLE NOT NULL;