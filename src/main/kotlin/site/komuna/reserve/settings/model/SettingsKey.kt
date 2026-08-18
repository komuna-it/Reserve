package site.komuna.reserve.settings.model

enum class SettingsKey(
    val defaultValue: String,
    val validator: Regex,
    val isSensitive: Boolean = false,
) {
    CONTACT_PHONE(
        defaultValue = "+48 555 245 156",
        validator = Regex(".*")
    ),
    CONTACT_EMAIL(
        defaultValue = "contact@vipsound.com",
        validator = Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$"),
    ),
    RESERVATION_CANCELLATION_WITHOUT_APPROVAL_HOURS(
        defaultValue = "24",
        validator = Regex("\\d+")
    ),
    RESERVATION_OPENING_HOUR(
        defaultValue = "8",
        validator = Regex("\\d+")
    ),
    RESERVATION_CLOSING_HOUR(
        defaultValue = "22",
        validator = Regex("\\d+")
    ),
    RESERVATION_REMINDER_HOUR(
        defaultValue = "12",
        validator = Regex("\\d+")
    ),
    MAIL_SERVER_HOST(
        defaultValue = "smtp.gmail.com",
        validator = Regex(".*"),
        isSensitive = true
    ),
    MAIL_SERVER_PORT(
        defaultValue = "587",
        validator = Regex("\\d+"),
        isSensitive = true
    ),
    MAIL_SERVER_USERNAME(
        defaultValue = "username@gmail.com",
        validator = Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$"),
        isSensitive = true
    ),
    MAIL_SERVER_PASSWORD(
        defaultValue = "applicationPassword",
        validator = Regex(".*"),
        isSensitive = true
    ),
    MAIL_SMTP_AUTH(
        defaultValue = "true",
        validator = Regex("true|false"),
        isSensitive = true
    ),
    MAIL_SMTP_STARTTLS_ENABLE(
        defaultValue = "true",
        validator = Regex("true|false"),
        isSensitive = true
    ),
    MAIL_SERVER_BETA_ADDRESS(
        defaultValue = "username@gmail.com",
        validator = Regex("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$"),
        isSensitive = true
    );
}