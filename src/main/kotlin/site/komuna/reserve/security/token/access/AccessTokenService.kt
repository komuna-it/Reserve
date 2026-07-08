package site.komuna.reserve.security.token.access

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import site.komuna.reserve.common.exception.InvalidRefreshToken
import site.komuna.reserve.security.token.TokenProperties
import site.komuna.reserve.security.token.refresh.RefreshToken
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import site.komuna.reserve.user.model.UserEntity
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.Date
import javax.crypto.SecretKey

@Service

class AccessTokenService(
    private val tokenProperties: TokenProperties,
    private val refreshTokenService: RefreshTokenService
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(tokenProperties.secret.toByteArray(StandardCharsets.UTF_8))
    private val expirationSeconds = tokenProperties.accessExpirationMinutes * 60

    fun generateAccessToken(user: UserEntity, refreshToken: String): AccessToken {
        if(!refreshTokenService.isTokenValid(refreshToken)) {
            throw InvalidRefreshToken()
        }

        val now = OffsetDateTime.now()
        val expires = now.plusSeconds(expirationSeconds)

        val token = Jwts.builder()
            .subject(user.id.toString())
            .claim("role", user.role.name)
            .issuedAt(Date.from(now.toInstant()))
            .expiration(Date.from(expires.toInstant()))
            .signWith(key)
            .compact()

        return AccessToken(
            token,
            expires
        )
    }

    fun generateAccessToken(user: UserEntity, refreshToken: RefreshToken) : AccessToken {
        return generateAccessToken(user, refreshToken.token)
    }
}