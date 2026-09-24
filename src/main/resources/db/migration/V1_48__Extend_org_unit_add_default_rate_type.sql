ALTER TABLE orgunits ADD COLUMN default_rate_id BIGINT NULL;
ALTER TABLE orgunits ADD CONSTRAINT fk_orgunits_default_rate
    FOREIGN KEY (default_rate_id) REFERENCES rates(id) ON DELETE SET NULL;
