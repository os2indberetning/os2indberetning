CREATE TABLE email_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email_to TEXT,
    email_cc TEXT,
    email_bcc TEXT,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    mail_type VARCHAR(255),
    cpr VARCHAR(255),
    person_uuid VARCHAR(255),
    perform_email_check BOOLEAN,
    delivery_tts TIMESTAMP
);

CREATE TABLE email_queue_attachment_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content MEDIUMBLOB
);

-- Create dependent tables afterwards
CREATE TABLE email_queue_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email_queue_id BIGINT,
    file_name VARCHAR(255),
    attachment_file_id BIGINT,
    FOREIGN KEY (email_queue_id) REFERENCES email_queue(id) ON DELETE CASCADE,
    FOREIGN KEY (attachment_file_id) REFERENCES email_queue_attachment_files(id) ON DELETE CASCADE
);

CREATE TABLE email_queue_inline_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email_queue_id BIGINT,
    base64 BOOLEAN,
    cid VARCHAR(255),
    src VARCHAR(255),
    FOREIGN KEY (email_queue_id) REFERENCES email_queue(id) ON DELETE CASCADE
);