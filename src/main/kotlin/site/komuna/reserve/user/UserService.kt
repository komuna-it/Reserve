package site.komuna.reserve.user

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.common.exception.ReserveException
import site.komuna.reserve.common.exception.UserNotFoundException
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import site.komuna.reserve.security.token.verification.VerificationTokenService
import site.komuna.reserve.user.ban.BanService
import site.komuna.reserve.user.ban.model.BanEntity
import site.komuna.reserve.user.model.UserEntity
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.Duration

@Service
class UserService(
    private val repository: UserRepository,
    private val validationTokenService: VerificationTokenService,
    private val passwordEncoder: PasswordEncoder,
    private val banService: BanService,
    private val refreshTokenService: RefreshTokenService,
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
        val targetUser = findById(id)
        val sourceUser = findById(by)

        if (role == Role.MANAGER) {
            logger.error { "User: ${sourceUser.email} tried to assign Manager role to user: ${targetUser.email}." }
            throw ReserveException(HttpStatus.FORBIDDEN, "You are not allowed to assign Manager role")
        }

        logger.info { "Assignee role $role to user: ${targetUser.email} by user ID: ${sourceUser.email}" }

        targetUser.role = role

        return repository.save(targetUser)
    }

    fun assigneeUserRole(id: Long, role: String, by: String): UserEntity {
        val role = Role.from(role.uppercase())
        return assigneeUserRole(id, role, by)
    }

    @Transactional
    fun banUser(id: Long, by: Long, reason: String, duration: Duration): BanEntity {
        val user = findById(id)
        val bannedBy = findById(by)

        if (reason.isBlank()) throw ReserveException(HttpStatus.BAD_REQUEST, "Reason is required")

        refreshTokenService.revokeAllTokensForUser(user)

        return banService.banUser(user, bannedBy, reason, duration)
    }

    fun findById(id: Long): UserEntity {
        return repository.findById(id).orElseThrow { UserNotFoundException(id) }
    }

    fun findById(id: String): UserEntity {
        return findById(id.toLong())
    }

    fun findByEmail(email: String): UserEntity {
        return repository.findByEmail(email).orElseThrow { UserNotFoundException(email) }
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

    fun isUserBanned(id: Long): Boolean {
        val ban = banService.isUserBanned(id)
        return ban != null
    }
}