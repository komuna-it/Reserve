package site.komuna.reserve.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.auth.request.LoginRequest
import site.komuna.reserve.auth.request.RegisterRequest
import site.komuna.reserve.auth.response.LoginResponse
import site.komuna.reserve.auth.response.RegisterResponse

@RestController
@RequestMapping("/auth")
class AuthController(
    private val service: AuthService
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest) : RegisterResponse{

        logger.info { "Received a request to create a new user with email: ${request.email} and nick: ${request.name}" }

        return service.register(request)

    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): LoginResponse {
        return service.login(request.email, request.password)
    }

    @PostMapping("/refresh")
    fun getAccessToken() {
        // TODO: Implement refresh token
    }
}