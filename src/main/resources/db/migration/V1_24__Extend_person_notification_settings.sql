ALTER TABLE persons ADD COLUMN receive_admin_mail     BOOLEAN DEFAULT FALSE;
ALTER TABLE persons ADD COLUMN receive_approver_mail  BOOLEAN DEFAULT FALSE;
ALTER TABLE persons ADD COLUMN receive_personal_mail  BOOLEAN DEFAULT FALSE;

CREATE TABLE deadline_emails (
    id                      BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    subject                 varchar(256) NOT NULL,
    message                 TEXT NOT NULL,
    first_notification      DATE NOT NULL,
    next_notification       DATE,
    repeating               BOOLEAN DEFAULT FALSE,
    last_sent               DATE
);