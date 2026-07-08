package site.komuna.reserve.security.token.refresh

import org.springframework.stereotype.Service
import site.komuna.reserve.common.extensions.sha256
import site.komuna.reserve.security.token.TokenProperties
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.util.UUID

@Service
class RefreshTokenService(
    private val repository: RefreshTokenRepository,
    private val tokenProperties: TokenProperties,
) {

    private val expirationDays = tokenProperties.refreshExpirationDays

    /**
     * Generate and save in the database a refresh token for a user
     */
    fun generateRefreshToken(user: UserEntity): RefreshToken {

        val token = UUID.randomUUID().toString()
        val hashedToken = token.sha256()

        val expires = OffsetDateTime.now().plusDays(expirationDays)

        repository.save(RefreshTokenEntity(
            token = hashedToken,
            user = user,
            expires = expires
        ))

        return RefreshToken(token, expires)
    }

    /**
     * Check if a refresh token is valid
     */
    fun isTokenValid(token: String): Boolean {
        val tokenEntity = repository.findByToken(token) ?: return false

        return tokenEntity.expires.isAfter(OffsetDateTime.now())
    }
}