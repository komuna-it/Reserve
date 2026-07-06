package site.komuna.reserve.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.komuna.reserve.auth.request.RegisterRequest

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @RequestMapping("/register")
    fun register(@RequestBody request: RegisterRequest) {
        logger.info { "Registering user: $request" }
    }
}