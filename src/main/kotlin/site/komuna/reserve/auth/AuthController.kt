package site.komuna.reserve.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.auth.request.LoginRequest
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.user.model.UserDto

@RestController
@RequestMapping("/auth")
class AuthController(
    private val service: AuthService
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest) : ResponseEntity<Void>{
        logger.info { "Received a request to create a new user with email: ${request.email} and nick: ${request.name}" }
        service.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest, response: HttpServletResponse): ResponseEntity<Long> {
        logger.info { "Triggered /login for email: ${request.email}" }
        val loginData = service.login(request.email, request.password)

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

        return ResponseEntity.ok(loginData.userId)
    }

    /**
     * Odświeżanie tokenu za pomocą ciasteczka HttpOnly.
     * Nowy AccessToken również wraca zabezpieczony w ciastku.
     */

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
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return try {
            val userStatus = service.getMe(authentication)
            ResponseEntity.ok(userStatus)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }
}