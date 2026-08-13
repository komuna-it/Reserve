package site.komuna.reserve.user

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.common.exception.InvalidCredentialsException
import site.komuna.reserve.common.exception.ReserveException
import site.komuna.reserve.common.exception.UserNotFoundException
import site.komuna.reserve.email.EmailService
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import site.komuna.reserve.security.token.verification.VerificationTokenService
import site.komuna.reserve.user.ban.BanService
import site.komuna.reserve.user.ban.model.BanDto
import site.komuna.reserve.user.ban.model.BanEntity
import site.komuna.reserve.user.model.UserDto
import site.komuna.reserve.user.model.UserEntity
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class UserService(
    private val repository: UserRepository,
    private val validationTokenService: VerificationTokenService,
    private val passwordEncoder: PasswordEncoder,
    private val banService: BanService,
    private val refreshTokenService: RefreshTokenService,
    private val emailService: EmailService,
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
        val preferredLanguage = request.preferredLanguage

        val savedUser = repository.save(
            UserEntity(
                email = email,
                nick = nick,
                password = password,
                role = role,
                created = now,
                passwordChanged = now,
                preferredLanguage = preferredLanguage
            )
        )

        logger.info { "User with email: ${savedUser.email} was created" }

        return savedUser
    }

    /**
     * Method assigns user role to a user
     */
    fun assigneeUserRole(id: Long, role: Role, by: String): UserEntity {
        val targetUser = findById(id)
        val sourceUser = findById(by)

        if (role == Role.MANAGER) {
            logger.error { "User: ${sourceUser.email} tried to assign Manager role to user: ${targetUser.email}." }
            throw ReserveException(HttpStatus.FORBIDDEN, "You are not allowed to assign Manager role")
        }

        if (role == Role.SYSTEM) {
            logger.error { "User: ${sourceUser.email} tried to assign System role to user: ${targetUser.email}." }
            throw ReserveException(HttpStatus.FORBIDDEN, "You are not allowed to assign System role")
        }

        logger.info { "Assignee role $role to user: ${targetUser.email} by user ID: ${sourceUser.email}" }

        targetUser.role = role

        return repository.save(targetUser)
    }

    fun assigneeUserRole(id: Long, roleStr: String, by: String): UserEntity {
        val role = Role.from(roleStr.uppercase())
        return assigneeUserRole(id, role, by)
    }

    fun assigneePreferredLanguage(id: Long, language: String): UserEntity {
        val user = findById(id)

        user.preferredLanguage = language
        return repository.save(user)
    }

    @Transactional
    fun banUser(id: Long, by: Long, reason: String, duration: Duration): BanEntity {
        val user = findById(id)
        val bannedBy = findById(by)

        if (reason.isBlank()) throw ReserveException(HttpStatus.BAD_REQUEST, "Reason is required")

        refreshTokenService.revokeAllTokensForUser(user)

        return banService.banUser(user, bannedBy, reason, duration)
    }

    fun unbanUser(userId: Long): UserEntity {
        val user = findById(userId)

        banService.unbanUser(user)
        return user
    }

    fun setTrusted(userID: Long, trusted: Boolean): UserEntity {
        val user = findById(userID)
        return setTrusted(user, trusted)
    }

    fun setTrusted(user: UserEntity, trusted: Boolean): UserEntity {
        user.trusted = trusted
        return repository.save(user)
    }

    fun updatePassword(userId: Long, currentPassword: String, newPassword: String): UserEntity {
        val user = findById(userId)

        return updatePassword(user, currentPassword, newPassword)
    }

    fun updatePassword(user: UserEntity, currentPassword: String, newPassword: String): UserEntity {
        if (!passwordEncoder.matches(currentPassword, user.password)) {
            throw InvalidCredentialsException()
        }

        user.password = passwordEncoder.encode(newPassword)
        user.passwordChanged = OffsetDateTime.now(ZoneOffset.UTC)
        val response = repository.save(user)

        val model = mutableMapOf<String, Any>()
        model["nick"] = user.nick

        emailService.sendEmailToUser(EmailTemplateType.CHANGED_PASSWORD, user, model)

        return response
    }

    // get methods
    fun findById(id: Long): UserEntity {
        return repository.findById(id).orElseThrow { UserNotFoundException(id) }
    }

    fun findById(id: String): UserEntity {
        return findById(id.toLong())
    }

    fun findByEmail(email: String): UserEntity {
        return repository.findByEmail(email).orElseThrow { UserNotFoundException(email) }
    }

    fun getSystemUser(): UserEntity {
        val users = repository.findByRole(Role.SYSTEM)

        if (users.isEmpty()) throw ReserveException(HttpStatus.NOT_FOUND, "System user not found")
        if (users.size > 1) throw ReserveException(
            HttpStatus.CONFLICT,
            "We have more than one system user. That should not happen"
        )

        return users[0]
    }

    fun getAllAdmins(): List<UserEntity> {
        val admins = repository.findByRole(Role.ADMIN)
        val managers = repository.findByRole(Role.MANAGER)
        return admins + managers
    }

    // Validation methods
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

    fun getUsers(pageable: Pageable): Page<UserDto> {
        return repository.findByNickNot("SYSTEM", pageable)
            .map { convertToUserDto(it) }
    }

    fun convertToUserDto(user: UserEntity): UserDto {
        val activeBan = banService.isUserBanned(user)
        val banDto = activeBan?.let { convertToBanDto(it) }

        return UserDto(
            userEntity = user,
            banDto = banDto
        )
    }

    fun convertToBanDto(ban: BanEntity): BanDto {
        val userDto = UserDto(userEntity = ban.user, banDto = null)
        val bannedByDto = UserDto(userEntity = ban.bannedBy, banDto = null)

        return BanDto(ban, userDto, bannedByDto)
    }
}