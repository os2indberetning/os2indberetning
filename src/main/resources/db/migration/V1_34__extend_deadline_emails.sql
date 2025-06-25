ALTER TABLE substitutes MODIFY substitute_exclusive_mode BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE deadline_emails ADD COLUMN deadline DATE;
