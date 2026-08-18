package site.komuna.reserve.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import site.komuna.reserve.auth.request.LoginRequest
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.common.httpError.exception.UserBannedException
import site.komuna.reserve.user.UserService
import site.komuna.reserve.user.ban.BanService
import site.komuna.reserve.user.model.UserDto

@RestController
@RequestMapping("/auth")
class AuthController(
    private val service: AuthService,
    private val userService: UserService,
    private val banService: BanService
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

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

        val accessTokenCookie = ResponseCookie.from("access_token", loginData.accessToken.token)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(java.time.Duration.ofMinutes(5))
            .sameSite("Lax")
            .build()

        val refreshEndpoint = "/auth/refresh"
        val refreshTokenCookie = ResponseCookie.from("refresh_token", loginData.refreshToken.token)
            .httpOnly(true)
            .secure(true)
            .path(refreshEndpoint)
            .maxAge(java.time.Duration.ofDays(7))
            .sameSite("Lax")
            .build()

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())

        return ResponseEntity.ok(userDto)
    }

    @PostMapping("/refresh")
    fun getAccessToken(
        @CookieValue("refresh_token") refreshToken: String,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val newAccessToken = service.refresh(refreshToken)

        val accessTokenCookie = ResponseCookie.from("access_token", newAccessToken.token)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(java.time.Duration.ofMinutes(5))
            .sameSite("Lax")
            .build()

        logger.info { "New access token: ${newAccessToken.token}" }

        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
        return ResponseEntity.ok().build()
    }

    @GetMapping("/confirmEmail/{verificationToken}")
    fun confirmEmail(@PathVariable verificationToken: String): ResponseEntity<Void> {
        service.confirmEmail(verificationToken)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication?): ResponseEntity<UserDto> {
        logger.trace { "/auth/me authentication: $authentication" }
        logger.trace { "Received a request to get current user" }

        if (authentication == null) {
            logger.trace { "authentication == null" }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return try {
            val userEntity = service.getMe(authentication)
            val userDto = userService.convertToUserDto(userEntity)

            ResponseEntity.ok(userDto)
        } catch (e: IllegalStateException) {
            logger.error { "IllegalStateException $e" }
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        val cleanAccess = ResponseCookie.from("access_token", "").httpOnly(true).path("/").maxAge(0).build()
        val cleanRefresh = ResponseCookie.from("refresh_token", "").httpOnly(true).path("/auth/refresh").maxAge(0).build()

        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccess.toString())
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefresh.toString())
        return ResponseEntity.ok().build()
    }
}