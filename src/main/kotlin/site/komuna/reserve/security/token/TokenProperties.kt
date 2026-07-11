package site.komuna.reserve.security.token

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tokens")
data class TokenProperties(
    val secret: String,
    var accessExpirationMinutes: Long = 2,
    var refreshExpirationDays: Long = 30,
    var validationExpirationMinutes: Long = 30,
) {
}