package site.komuna.reserve.security.token.access

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import site.komuna.reserve.common.httpError.exception.InvalidRefreshTokenException
import site.komuna.reserve.security.token.TokenProperties
import site.komuna.reserve.security.token.refresh.RefreshTokenEntity
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import site.komuna.reserve.user.model.UserEntity
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import javax.crypto.SecretKey

@Service

class AccessTokenService(
    private val tokenProperties: TokenProperties,
    private val refreshTokenService: RefreshTokenService
) {

    private val key: SecretKey = Keys.hmacShaKeyFor(tokenProperties.secret.toByteArray(StandardCharsets.UTF_8))
    private val expirationSeconds = tokenProperties.accessExpirationMinutes * 60

    // GENERATING ACCESS TOKEN
    fun generateAccessToken(user: UserEntity, refreshToken: String): AccessToken {
        if(!refreshTokenService.isTokenValid(refreshToken)) {
            throw InvalidRefreshTokenException()
        }

        val now = OffsetDateTime.now(ZoneOffset.UTC)
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

    fun generateAccessToken(user: UserEntity, refreshToken: RefreshTokenEntity) : AccessToken {
        return generateAccessToken(user, refreshToken.token)
    }

    fun generateAccessToken(refreshToken: String) : AccessToken {
        val token = refreshTokenService.getRefreshToken(refreshToken) ?: throw InvalidRefreshTokenException()

        return generateAccessToken(token.user, refreshToken)
    }

    // EXTRACT CLAIMS
    fun extractUserId(token: String): Long {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
            .subject.toLong()
    }

    fun extractUserRole(token: String): String {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims["role"] as String
    }
}