package site.komuna.reserve.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import site.komuna.reserve.auth.request.LoginRequest
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.common.httpError.exception.UserBannedException
import site.komuna.reserve.security.token.TokenProperties
import site.komuna.reserve.security.token.access.AccessTokenService
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.ban.BanService
import site.komuna.reserve.user.model.UserDto
import java.time.Duration
import java.time.Instant

@RestController
@RequestMapping("/auth")
class AuthController(
    private val service: AuthService,
    private val accessTokenService: AccessTokenService,
    private val userService: UserService,
    private val banService: BanService,
    private val tokenProperties: TokenProperties
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val accessTokenDuration: Duration
        get() = Duration.ofMinutes(tokenProperties.accessExpirationMinutes)

    private val refreshTokenDuration: Duration
        get() = Duration.ofDays(tokenProperties.refreshExpirationDays)


    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Void> {
        logger.info { "Received a request to create a new user with email: ${request.email} and nick: ${request.name}" }
        service.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }


    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, response: HttpServletResponse): ResponseEntity<*> {
        logger.info { "Triggered /login for email: ${request.email}" }

        val userEntity = userService.findByEmail(request.email)
        val activeBan = banService.isUserBanned(userEntity)

        if (activeBan != null) {
            logger.warn { "User ${userEntity.id} is banned until ${activeBan.banExpires}" }

            throw UserBannedException(activeBan.banExpires)
        }

        val loginData = service.login(request.email, request.password)
        val userDto = userService.convertToUserDto(userEntity)
        userDto.accessTokenExpiresAt = Instant.now().plus(accessTokenDuration).toEpochMilli()

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            service.createTokenCookie(
                "access_token",
                loginData.accessToken.token,
                accessTokenDuration
            ).toString()
        )

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            service.createTokenCookie(
                "refresh_token",
                loginData.refreshToken.token,
                refreshTokenDuration
            ).toString()
        )
        return ResponseEntity.ok(userDto)
    }

    @PostMapping("/refresh")
    fun getAccessToken(
        @CookieValue("refresh_token", required = false) refreshToken: String?,
        response: HttpServletResponse
    ): ResponseEntity<Map<String, Long>> {

        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val newAccessToken = service.refresh(refreshToken)

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            service.createTokenCookie(
                "access_token",
                newAccessToken.token,
                accessTokenDuration
            ).toString()
        )

        val newExpiresAt = Instant.now()
            .plus(accessTokenDuration)
            .toEpochMilli()

        return ResponseEntity.ok(
            mapOf("accessTokenExpiresAt" to newExpiresAt)
        )
    }

    @GetMapping("/confirmEmail/{verificationToken}")
    fun confirmEmail(@PathVariable verificationToken: String): ResponseEntity<Void> {
        service.confirmEmail(verificationToken)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/me")
    fun getCurrentUser(
        authentication: Authentication?,
        @CookieValue("access_token", required = false) accessToken: String?,
        @CookieValue("refresh_token", required = false) refreshToken: String?
    ): ResponseEntity<UserDto?> {

        if (authentication == null || !authentication.isAuthenticated) {
            // no refresh token --  anonymous session.
            if (refreshToken.isNullOrBlank()) {
                return ResponseEntity.ok(null)
            }

            //  refresh token exists, so the frontend can attempt restoration
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return try {
            val userEntity = service.getMe(authentication)
            val userDto = userService.convertToUserDto(userEntity)

            if (accessToken != null) {
                userDto.accessTokenExpiresAt =
                    accessTokenService.getExpirationMillis(accessToken)
            }

            ResponseEntity.ok(userDto)

        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            service.createTokenCookie("access_token", "", Duration.ZERO).toString()
        )

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            service.createTokenCookie("refresh_token", "", Duration.ZERO).toString()
        )
        return ResponseEntity.ok().build()
    }
}