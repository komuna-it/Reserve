
package site.komuna.reserve.auth

import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.audit.AuditService
import site.komuna.reserve.audit.model.AuditType
import site.komuna.reserve.audit.model.AuthAuditContext
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.auth.response.LoginResponse
import site.komuna.reserve.common.httpError.exception.EmailNotConfirmedException
import site.komuna.reserve.common.httpError.exception.EmailTakenException
import site.komuna.reserve.common.httpError.exception.InvalidCredentialsException
import site.komuna.reserve.email.EmailService
import site.komuna.reserve.email.model.EmailRecipient
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.security.token.access.AccessToken
import site.komuna.reserve.security.token.access.AccessTokenService
import site.komuna.reserve.security.token.refresh.RefreshToken
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import site.komuna.reserve.security.token.verification.VerificationTokenService
import site.komuna.reserve.settings.SettingsService
import site.komuna.reserve.settings.model.SettingsKey
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity
import java.time.Duration

@Service
class AuthService(
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val verificationTokenService: VerificationTokenService,
    private val settings: SettingsService,
    private val emailService: EmailService,
    private val auditService: AuditService,
) {

    fun register(
        request: RegisterRequest,
        context: AuthAuditContext,
    ) {
        if (userService.isEmailTaken(request.email)) {
            auditService.log(
                type = AuditType.USER_REGISTER_FAILED,
                properties = context.toProperties() + mapOf(
                    "email" to request.email,
                    "reason" to "EMAIL_ALREADY_TAKEN",
                )
            )
            throw EmailTakenException()
        }

        val newUser = userService.createUser(request)

        auditService.log(
            type = AuditType.USER_REGISTER_SUCCESS,
            actor = newUser,
            properties = context.toProperties() + mapOf(
                "userId" to newUser.id,
                "email" to newUser.email,
                "role" to newUser.role.name,
            )
        )

        val verificationToken =
            verificationTokenService.generateVerificationTokenEntity(newUser)

        val model = mutableMapOf<String, Any>()
        val recipient = EmailRecipient(newUser)

        val domain = settings.getStringValue(SettingsKey.FRONTEND_URL)
        val link = settings.getStringValue(SettingsKey.CONFIRMATION_EMAIL_LINK)

        model["link"] = domain + link + verificationToken.token

        emailService.sendEmailToUser(
            EmailTemplateType.ACTIVATION_EMAIL,
            recipient,
            model
        )
    }

    fun login(
        email: String,
        password: String,
        context: AuthAuditContext,
    ): LoginResponse {
        val user = try {
            authenticate(email, password)
        } catch (exception: InvalidCredentialsException) {
            auditService.log(
                type = AuditType.USER_LOGIN_FAILED,
                properties = context.toProperties() + mapOf(
                    "email" to email,
                    "reason" to "INVALID_CREDENTIALS",
                )
            )
            throw exception
        }

        if (!userService.wasEmailConfirmed(user)) {
            auditService.log(
                type = AuditType.USER_LOGIN_FAILED,
                properties = context.toProperties() + mapOf(
                    "userId" to user.id,
                    "email" to user.email,
                    "reason" to "EMAIL_NOT_CONFIRMED",
                )
            )
            throw EmailNotConfirmedException(email)
        }

        val refreshToken = refreshTokenService.generateRefreshToken(user)
        val accessToken = accessTokenService.generateAccessToken(user, refreshToken)

        auditService.log(
            type = AuditType.USER_LOGIN_SUCCESS,
            actor = user,
            properties = context.toProperties() + mapOf(
                "userId" to user.id,
                "email" to user.email,
                "role" to user.role.name,
                "authenticationMethod" to "PASSWORD",
            )
        )

        return LoginResponse(
            RefreshToken(refreshToken),
            accessToken,
            user,
        )
    }

    fun refresh(
        token: String,
        context: AuthAuditContext,
    ): AccessToken {
        val accessToken = accessTokenService.generateAccessToken(token)

        auditService.log(
            type = AuditType.USER_SESSION_REFRESHED,
            properties = context.toProperties() + mapOf(
                "result" to "SUCCESS",
            )
        )

        return accessToken
    }

    fun confirmEmail(
        token: String,
        context: AuthAuditContext,
    ) {
        verificationTokenService.confirmEmail(token)

        auditService.log(
            type = AuditType.USER_EMAIL_VERIFIED,
            properties = context.toProperties() + mapOf(
                "result" to "SUCCESS",
            )
        )
    }

    private fun authenticate(email: String, password: String): UserEntity {
        val user = userService.findByEmail(email)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(password, user.password)) {
            throw InvalidCredentialsException()
        }

        return user
    }

    fun getMe(authentication: Authentication?): UserEntity {
        if (authentication == null || !authentication.isAuthenticated) {
            throw IllegalStateException("user was not authenticated")
        }

        val id = authentication.principal
        return userService.findById(id as Long)
    }

    fun createTokenCookie(
        name: String,
        value: String,
        duration: Duration
    ): ResponseCookie {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(duration)
            .build()
    }
}
