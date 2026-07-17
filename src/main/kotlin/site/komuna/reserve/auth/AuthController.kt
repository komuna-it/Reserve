package site.komuna.reserve.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.auth.request.LoginRequest
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.auth.response.LoginResponse
import site.komuna.reserve.security.token.access.AccessToken
import kotlin.math.log
import kotlin.time.Duration

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
    fun login(@RequestBody request: LoginRequest, response: HttpServletResponse): ResponseEntity<String> {

        println("Triggered /login")
        val loginData = service.login(request.email, request.password)
        println("LoginData: $loginData")

        // TODO
        // token duration by env var!!

        val accessTokenCookie = ResponseCookie.from("access_token", loginData.accessToken.token)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(java.time.Duration.ofMinutes(5))
            .sameSite("Lax")
            .build()

        // TODO
        // token duration by env var!!
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
        println("accessTokenCookie:  $accessTokenCookie")
        println("refreshTokenCookie:  $refreshTokenCookie.toString()")


        return ResponseEntity.ok(loginData.userNick)
    }

    @PostMapping("/refresh")
    fun getAccessToken(@PathVariable token: String): ResponseEntity<AccessToken>{
        val accessToken = service.refresh(token)
        return ResponseEntity.ok(accessToken)
    }

    @GetMapping("/confirmEmail/{verificationToken}")
    fun confirmEmail(@PathVariable verificationToken: String): ResponseEntity<Void> {
        service.confirmEmail(verificationToken)
        logger.info { "Potwierdzanie adresu e-mail dla tokenu: $verificationToken" }
        return ResponseEntity.ok().build()
    }

}