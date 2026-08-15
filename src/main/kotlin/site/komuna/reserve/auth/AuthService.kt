package site.komuna.reserve.auth

import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.auth.response.LoginResponse
import site.komuna.reserve.common.exception.EmailAlreadyTakenException
import site.komuna.reserve.common.exception.EmailNotConfirmedException
import site.komuna.reserve.common.exception.InvalidCredentialsException
import site.komuna.reserve.common.exception.TokenExpiredException
import site.komuna.reserve.email.EmailService
import site.komuna.reserve.email.model.EmailRecipient
import site.komuna.reserve.email.model.EmailTemplateType
import site.komuna.reserve.security.token.access.AccessToken
import site.komuna.reserve.security.token.access.AccessTokenService
import site.komuna.reserve.security.token.refresh.RefreshToken
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import site.komuna.reserve.security.token.verification.VerificationTokenService
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity

@Service
class AuthService (
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val verificationTokenService: VerificationTokenService,
    private val emailService: EmailService,
) {

    fun register(request: RegisterRequest) {
        if(userService.isEmailTaken(request.email)) {
            throw EmailAlreadyTakenException()
        }

        val newUser = userService.createUser(request)
        val verificationToken = verificationTokenService.generateVerificationTokenEntity(newUser)
        val model = mutableMapOf<String, Any>()

        val recipient = EmailRecipient(newUser)
        model["verificationToken"] = verificationToken.token

        emailService.sendEmailToUser(EmailTemplateType.ACTIVATION_EMAIL, recipient, model)
    }

    fun login(email: String, password: String) : LoginResponse {
        val user = authenticate(email, password)

        if(!userService.wasEmailConfirmed(user)) throw EmailNotConfirmedException()

        val refreshToken = refreshTokenService.generateRefreshToken(user)
        val accessToken = accessTokenService.generateAccessToken(user, refreshToken)

        val refreshDto = RefreshToken(refreshToken)

        return LoginResponse(refreshDto, accessToken, user)
    }

    fun refresh(token: String): AccessToken {
        return accessTokenService.generateAccessToken(token)
    }

    fun confirmEmail(token: String) {
        try {
            verificationTokenService.confirmEmail(token)
        }
        catch (e: Exception) {
            verificationTokenService.regenerateVerificationToken(token)
            throw TokenExpiredException()
        }
    }

    private fun authenticate(email: String, password: String): UserEntity {
        val user = userService.findByEmail(email) ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(password, user.password)) {
            throw InvalidCredentialsException()
        }

        return user
    }

    // get user from session
    fun getMe(authentication: Authentication?): UserEntity {
        if (authentication == null || !authentication.isAuthenticated) {
            throw IllegalStateException("user was not authenticated")
        }

        val id = authentication.principal
        val userEntity = userService.findById(id as Long)

        return userEntity
    }
}
