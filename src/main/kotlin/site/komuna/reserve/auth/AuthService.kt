package site.komuna.reserve.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.auth.response.LoginResponse
import site.komuna.reserve.auth.response.RegisterResponse
import site.komuna.reserve.common.exception.EmailAlreadyTakenException
import site.komuna.reserve.common.exception.InvalidCredentialsException
import site.komuna.reserve.security.token.access.AccessTokenService
import site.komuna.reserve.security.token.refresh.RefreshTokenService
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.model.UserEntity

@Service
class AuthService (
    private val userService: UserService,
    private val passwordEncoder: PasswordEncoder,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService
) {

    fun register(request: RegisterRequest) : RegisterResponse {

        if(userService.isEmailTaken(request.email)) {
            throw EmailAlreadyTakenException()
        }

        // TODO: Dokończyć rejestracje

        val newUser = userService.createUser(request)

        return RegisterResponse()
    }

    fun login(email: String, password: String) : LoginResponse {
        val user = authenticate(email, password)

        val refreshToken = refreshTokenService.generateRefreshToken(user)
        val accessToken = accessTokenService.generateAccessToken(user, refreshToken)

        return LoginResponse(refreshToken, accessToken)
    }

    /**
     * Method check if a user exists and the password is correct
     */
    private fun authenticate(email: String, password: String): UserEntity {
        val user = userService.findByEmail(email)?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(password, user.password)) {
            throw InvalidCredentialsException()
        }

        return user
    }
}