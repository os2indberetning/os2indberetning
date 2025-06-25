ALTER TABLE addresses ADD COLUMN created_timestamp datetime NOT NULL;
ALTER TABLE addresses ADD COLUMN coordinate_fetch_tries INT NOT NULL DEFAULT 0;
