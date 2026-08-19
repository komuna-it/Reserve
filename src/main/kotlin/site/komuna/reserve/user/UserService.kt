package site.komuna.reserve.user

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.common.httpError.exception.CannotPerformThatActionException
import site.komuna.reserve.common.httpError.exception.InternalServerException
import site.komuna.reserve.common.httpError.exception.InvalidCredentialsException
import site.komuna.reserve.common.httpError.exception.UserNotFoundException
import site.komuna.reserve.email.EmailService
import site.komuna.reserve.email.model.EmailRecipient
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import site.komuna.reserve.security.token.verification.VerificationTokenService
import site.komuna.reserve.user.ban.BanService
import site.komuna.reserve.user.ban.model.BanDto
import site.komuna.reserve.user.ban.model.BanEntity
import site.komuna.reserve.user.model.UserDto
import site.komuna.reserve.user.model.UserEntity
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

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
        val password = passwordEncoder.encode(request.password)

        val savedUser = repository.save(
            UserEntity(
                email = email,
                nick = request.name,
                password = password,
                role = Role.USER,
                created = now,
                passwordChanged = now,
                preferredLanguage = request.preferredLanguage
            )
        )

        logger.trace { "Created a new user" }
        logger.trace { "ID: ${savedUser.id}, Email: ${savedUser.email}, Nick: ${savedUser.nick}" }

        return savedUser
    }

    @Transactional
    fun deleteAccount(userId: Long) {
        val user = findById(userId)

        deleteAccount(user)
    }

    fun deleteAccount(user: UserEntity) {
        logger.trace { "User ${user.email} requested to delete their account" }

        user.nick = "DELETED"
        user.trusted = false
        user.role = Role.ORPHAN
        user.email = "DELETED"
        user.password = ""

        refreshTokenService.revokeAllTokensForUser(user)

        repository.save(user)
        logger.trace { "User ${user.email} account deleted"}
    }

    /**
     * Method assigns a role to a user.
     */
    fun assigneeUserRole(id: Long, role: Role, by: String): UserEntity {
        val targetUser = findById(id)
        val sourceUser = findById(by)

        if (role == Role.MANAGER) {
            logger.warn { "User: ${sourceUser.email} tried to assign Manager role to user: ${targetUser.email}." }
            throw CannotPerformThatActionException("")
        }

        if (role == Role.SYSTEM) {
            logger.warn { "User: ${sourceUser.email} tried to assign System role to user: ${targetUser.email}." }
            throw CannotPerformThatActionException("")
        }

        logger.trace { "Assignee role $role to user: ${targetUser.email} by user ID: ${sourceUser.email}" }

        targetUser.role = role

        return repository.save(targetUser)
    }

    fun assigneeUserRole(id: Long, roleStr: String, by: String): UserEntity {
        val role = Role.from(roleStr.uppercase())
        return assigneeUserRole(id, role, by)
    }

    /**
     * UI and emails are created based on a preferred language
     */
    fun assigneePreferredLanguage(id: Long, language: String): UserEntity {
        val user = findById(id)

        user.preferredLanguage = language
        val response = repository.save(user)

        logger.trace { "Preferred language for user: ${user.email} set to: ${user.preferredLanguage}" }
        return response
    }

    // BAN
    fun isUserBanned(id: Long): Boolean {
        val ban = banService.isUserBanned(id)
        return ban != null
    }

    @Transactional
    fun banUser(id: Long, by: Long, reason: String, duration: Duration): BanEntity {
        val user = findById(id)
        val bannedBy = findById(by)

        val expires = OffsetDateTime.now(ZoneOffset.UTC) + duration
        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy HH:mm", Locale.of("pl", "PL"))
        val formattedExpires = expires.format(formatter)

        refreshTokenService.revokeAllTokensForUser(user)

        val recipient = EmailRecipient(user)
        val model = mutableMapOf<String, Any>()
        model["reason"] = reason
        model["expires"] = formattedExpires

        emailService.sendEmailToUser(EmailTemplateType.USER_BANNED, recipient, model)

        return banService.banUser(user, bannedBy, reason, duration)
    }

    fun unbanUser(userId: Long, requestedBy: Long): UserEntity {
        val user = findById(userId)
        val requestedUser = findById(requestedBy)

        banService.unbanUser(user, requestedUser)
        return user
    }

    // TRUSTED
    fun setTrusted(userID: Long, trusted: Boolean): UserEntity {
        val user = findById(userID)
        return setTrusted(user, trusted)
    }

    fun setTrusted(user: UserEntity, trusted: Boolean): UserEntity {
        user.trusted = trusted
        return repository.save(user)
    }

    // PASSWORD
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
        val recipient = EmailRecipient(user)

        emailService.sendEmailToUser(EmailTemplateType.CHANGED_PASSWORD, recipient, model)

        return response
    }

    fun forgotPassword(email: String) {
        val user = findByEmail(email)

        val newPassword = generatePassword()
        val encodedPassword = passwordEncoder.encode(newPassword)

        user.password = encodedPassword
        user.passwordChanged = OffsetDateTime.now(ZoneOffset.UTC)

        val model = mutableMapOf<String, Any>()
        model["nick"] = user.nick
        model["newPassword"] = newPassword

        val recipient = EmailRecipient(user)
        emailService.sendEmailToUser(EmailTemplateType.REMIND_PASSWORD, recipient, model)

        repository.save(user)
    }

    fun generatePassword(length: Int = 16): String {
        require(length >= 3) {
            "Password length must be at least 3"
        }

        val random = SecureRandom()

        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val all = upper + lower + digits

        val chars = mutableListOf(
            upper[random.nextInt(upper.length)],
            lower[random.nextInt(lower.length)],
            digits[random.nextInt(digits.length)]
        )

        repeat(length - chars.size) {
            chars += all[random.nextInt(all.length)]
        }

        return chars.shuffled(random).joinToString("")
    }

    // GET METHODS
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

        if (users.isEmpty() || users.size > 1) {
            logger.error { "System user not found or more than one user with system role" }
            throw InternalServerException()
        }

        return users[0]
    }

    fun getUsers(pageable: Pageable): Page<UserDto> {
        return repository.findByNickNot("SYSTEM", pageable)
            .map { convertToUserDto(it) }
    }

    fun getAllAdmins(): List<UserEntity> {
        val admins = repository.findByRole(Role.ADMIN)
        val managers = repository.findByRole(Role.MANAGER)
        return admins + managers
    }

    // VALIDATION
    fun isEmailTaken(email: String): Boolean {
        return repository.existsUserEntityByEmail(email)
    }


    /**
     * Email was confirmed if table verification_tokens doesn't contain a token for this user
     */
    fun wasEmailConfirmed(user: UserEntity): Boolean {
        return validationTokenService.getTokenForUser(user) == null
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