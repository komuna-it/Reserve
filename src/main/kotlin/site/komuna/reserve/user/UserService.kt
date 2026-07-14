package site.komuna.reserve.user

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.common.exception.UserNotFoundException
import site.komuna.reserve.security.token.verification.VerificationTokenService
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class UserService(
    private val repository: UserRepository,
    private val validationTokenService: VerificationTokenService,
    private val passwordEncoder: PasswordEncoder
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    fun createUser(request: RegisterRequest): UserEntity {

        val now = OffsetDateTime.now(ZoneOffset.UTC)
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

        logger.info { "User with email: ${savedUser.email} was created" }

        return savedUser
    }

    /**
     * Method assignee user role to a user
     */
    fun assigneeUserRole(id: Long, role: Role, by: String): UserEntity {
        val targetUser = findById(id) ?: throw UserNotFoundException(id)
        val sourceUser = findById(by.toLong()) ?: throw UserNotFoundException(by.toLong())

        logger.info { "Assignee role $role to user: ${targetUser.email} by user ID: ${sourceUser.email}" }

        targetUser.role = role

        return repository.save(targetUser)
    }

    fun assigneeUserRole(id: Long, role: String, by: String): UserEntity {
        val role = Role.from(role.uppercase())
        return assigneeUserRole(id, role, by)
    }

    fun findById(id: Long): UserEntity? {
        return repository.findById(id).orElse(null)
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