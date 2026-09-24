ALTER TABLE orgunits ADD COLUMN default_rate_type_id BIGINT NULL;

-- Migrate old data from OUs: resolve rate → rate_type
UPDATE orgunits o
    JOIN rates r ON r.id = o.default_rate_id
    SET o.default_rate_type_id = r.rate_type_id
    WHERE o.default_rate_id IS NOT NULL;

-- Add FK on the new column
ALTER TABLE orgunits ADD CONSTRAINT fk_orgunits_default_rate_type_id
    FOREIGN KEY (default_rate_type_id) REFERENCES rate_types(id) ON DELETE SET NULL;

-- Drop old FK and column
ALTER TABLE orgunits DROP CONSTRAINT fk_orgunits_default_rate;
ALTER TABLE orgunits DROP COLUMN default_rate_id;

-- Reports: add km_rate_type_id column
ALTER TABLE reports ADD COLUMN km_rate_type_id BIGINT NULL;

