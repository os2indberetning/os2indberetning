-- 1. Merge duplicate (person, address) rows before adding the unique constraint.
--    Keep the row with the highest count per group; sum counts and take the latest last_drive_date.
UPDATE sixty_day_rules s
    INNER JOIN (
        SELECT
            MIN(id)                  AS keep_id,
            person,
            address,
            SUM(`count`)             AS total_count,
            MAX(last_drive_date)     AS latest_date
        FROM sixty_day_rules
        GROUP BY person, LOWER(TRIM(address))
        HAVING COUNT(*) > 1
    ) g ON s.person = g.person AND LOWER(TRIM(s.address)) = LOWER(TRIM(g.address)) AND s.id = g.keep_id
SET s.count = g.total_count,
    s.last_drive_date = g.latest_date;

DELETE s FROM sixty_day_rules s
    INNER JOIN (
        SELECT MIN(id) AS keep_id, person, LOWER(TRIM(address)) AS norm_address
        FROM sixty_day_rules
        GROUP BY person, LOWER(TRIM(address))
    ) g ON s.person = g.person AND LOWER(TRIM(s.address)) = g.norm_address
WHERE s.id != g.keep_id;

-- 2. Rename column and fix type: person VARCHAR(255) -> person_id BIGINT with proper FK.
ALTER TABLE sixty_day_rules
    CHANGE COLUMN person person_id BIGINT NOT NULL;

ALTER TABLE sixty_day_rules
    ADD CONSTRAINT fk_sixty_day_rules_person
        FOREIGN KEY (person_id) REFERENCES persons(id);

-- 3. Add unique constraint so duplicates can never be inserted again.
ALTER TABLE sixty_day_rules
    ADD CONSTRAINT uq_sixty_day_rules_person_address UNIQUE (person_id, address);
