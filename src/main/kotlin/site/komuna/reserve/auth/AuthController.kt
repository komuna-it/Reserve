package site.komuna.reserve.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    fun login(@RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val response = service.login(request.email, request.password)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/refresh/{token}")
    fun getAccessToken(@PathVariable token: String): ResponseEntity<AccessToken>{
        val accessToken = service.refresh(token)
        return ResponseEntity.ok(accessToken)
    }

    @GetMapping("/confirmEmail/{verificationToken}")
    fun confirmEmail(@PathVariable verificationToken: String): ResponseEntity<Void> {
        service.confirmEmail(verificationToken)

        return ResponseEntity.ok().build()
    }

}