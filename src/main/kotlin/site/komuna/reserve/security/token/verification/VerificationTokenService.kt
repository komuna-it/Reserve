package site.komuna.reserve.security.token.verification

import org.springframework.stereotype.Service
import site.komuna.reserve.common.httpError.exception.EmailAlreadyVerifiedException
import site.komuna.reserve.security.token.TokenProperties
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class VerificationTokenService (
    private val repository: VerificationTokenRepository,
    private val tokenProperties: TokenProperties,
){
    private val expirationMinutes = tokenProperties.validationExpirationMinutes

    /**
     * Method create a new token for a user and save it in to the database
     * it means that the provided user did not confirm his email yet
     */
    fun generateVerificationTokenEntity(user: UserEntity): VerificationTokenEntity {
        val token = UUID.randomUUID().toString()

        val created = OffsetDateTime.now(ZoneOffset.UTC)
        val expires = created.plusMinutes(expirationMinutes)

        val newToken = repository.save(VerificationTokenEntity(
            token = token,
            user = user,
            expires = expires,
            createdAt = created
        ))

        return newToken
    }

    /**
     * Method regenerate a token for a user.
     */
    fun regenerateVerificationToken(token: String): VerificationTokenEntity {
        val tokenEntity = repository.findByToken(token)!! // <- we know that the token exists

        val token = UUID.randomUUID().toString()

        val created = OffsetDateTime.now(ZoneOffset.UTC)
        val expires = created.plusMinutes(expirationMinutes)

        tokenEntity.token = token
        tokenEntity.expires = expires
        tokenEntity.createdAt = created

        return repository.save(tokenEntity)
    }

    fun getTokenForUser(user: UserEntity): VerificationTokenEntity? {
        return repository.findByUser(user)
    }

    fun confirmEmail(token: String) {
        val tokenEntity = repository.findByToken(token) ?: throw EmailAlreadyVerifiedException()

        // Removed token expiration check. Not important from a business perspective at this point

        // val now = OffsetDateTime.now(ZoneOffset.UTC)
        // if(now.isAfter(tokenEntity.expires)) throw EmailVerificationTokenExpiredException()

        removeToken(tokenEntity)
    }

    /**
     * Removing a token from the database means that the user has confirmed his email
     */
    fun removeToken(token: VerificationTokenEntity) {
        repository.delete(token)
    }
}