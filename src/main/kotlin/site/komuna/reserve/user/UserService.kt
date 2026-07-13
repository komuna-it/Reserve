package site.komuna.reserve.user

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.common.exception.VerificationTokenNotFoundException
import site.komuna.reserve.security.token.verification.VerificationTokenService
import site.komuna.reserve.user.model.UserDto
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime

@Service
class UserService(
    private val repository: UserRepository,
    private val validationTokenService: VerificationTokenService,
    private val passwordEncoder: PasswordEncoder
) {
    fun createUser(request: RegisterRequest): UserEntity {

        val now = OffsetDateTime.now()
        val email = request.email
        val nick = request.name
        val password = passwordEncoder.encode(request.password)
        val role = Role.USER

        val savedUser = repository.save(UserEntity(
            email = email,
            nick = nick,
            password = password,
            role = role,
            created = now,
            passwordChanged = now
        ))

        return savedUser
    }

    fun findByEmail(email: String): UserEntity? {
        return repository.findByEmail(email)
    }

    fun isEmailTaken(email: String): Boolean {
        return repository.existsUserEntityByEmail(email)
    }

    /**
     * Email was confirmed if table verification_tokens doesn't contain a token for this user
     */
    fun wasEmailConfirmed(user: UserEntity): Boolean {
        return validationTokenService.getTokenForUser(user) == null
    }
}