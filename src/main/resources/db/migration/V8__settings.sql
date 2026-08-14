ALTER TABLE settings
    DROP CONSTRAINT settings_key_check;

ALTER TABLE settings ADD CONSTRAINT settings_key_check
    CHECK(
        key IN (
            'RESERVATION_CANCELLATION_WITHOUT_APPROVAL_HOURS',
            'RESERVATION_OPENING_HOUR',
            'RESERVATION_CLOSING_HOUR',
            'RESERVATION_REMINDER_HOUR',
            'MAIL_SERVER_HOST',
            'MAIL_SERVER_PORT',
            'MAIL_SERVER_USERNAME',
            'MAIL_SERVER_PASSWORD',
            'MAIL_SMTP_AUTH',
            'MAIL_SMTP_STARTTLS_ENABLE',
            'MAIL_SERVER_BETA_ADDRESS'
        )
    );