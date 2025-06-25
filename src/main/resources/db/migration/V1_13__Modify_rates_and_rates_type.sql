ALTER TABLE rate_types ADD COLUMN pay_type INT NOT NULL;
ALTER TABLE rate_types ADD COLUMN sequential_number INT;

ALTER TABLE rates DROP COLUMN pay_type;
ALTER TABLE rates DROP COLUMN sequential_number;

ALTER TABLE reports ADD COLUMN active_year INT;