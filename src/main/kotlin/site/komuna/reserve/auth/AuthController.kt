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
import jakarta.servlet.http.HttpServletRequest
import site.komuna.reserve.audit.AuditService
import site.komuna.reserve.audit.model.AuditType
import site.komuna.reserve.audit.model.AuthAuditContext


@RestController
@RequestMapping("/auth")
class AuthController(
    private val service: AuthService,
    private val accessTokenService: AccessTokenService,
    private val userService: UserService,
    private val banService: BanService,
    private val tokenProperties: TokenProperties,
    private val auditService: AuditService,

    ) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val accessTokenDuration: Duration
        get() = Duration.ofMinutes(tokenProperties.accessExpirationMinutes)

    private val refreshTokenDuration: Duration
        get() = Duration.ofDays(tokenProperties.refreshExpirationDays)


    @PostMapping("/register")
    fun register(    @Valid @RequestBody request: RegisterRequest,
                     httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {
        logger.info { "Received a request to create a new user with email: ${request.email} and nick: ${request.name}" }
        service.register(request, auditContext(httpRequest))
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }


    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        response: HttpServletResponse,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<*> {
        val context = auditContext(httpRequest)

        val userEntity = userService.findByEmail(request.email)

        val activeBan = userEntity.let {
            banService.isUserBanned(it)
        }

        if (activeBan != null) {
            auditService.log(
                type = AuditType.USER_LOGIN_BLOCKED,
                actor = userEntity,
                properties = context.toProperties() + mapOf(
                    "userId" to userEntity.id,
                    "email" to userEntity.email,
                    "reason" to "ACTIVE_BAN",
                    "banExpires" to activeBan.banExpires.toString(),
                )
            )

            logger.warn { "Login attempt blocked for banned user ${userEntity.id}" }

            throw UserBannedException(activeBan.banExpires)
        }

        val loginData = service.login(
            request.email,
            request.password,
            context,
        )

        val authenticatedUser = userService.findById(loginData.userId)
        val userDto = userService.convertToUserDto(authenticatedUser)

        userDto.accessTokenExpiresAt =
            Instant.now().plus(accessTokenDuration).toEpochMilli()

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            service.createTokenCookie(
                "access_token",
                loginData.accessToken.token,
                accessTokenDuration,
            ).toString()
        )

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            service.createTokenCookie(
                "refresh_token",
                loginData.refreshToken.token,
                refreshTokenDuration,
            ).toString()
        )

        return ResponseEntity.ok(userDto)
    }

    @PostMapping("/refresh")
    fun getAccessToken(
        @CookieValue("refresh_token", required = false) refreshToken: String?,
        response: HttpServletResponse,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Map<String, Long>> {

        val context = auditContext(httpRequest)

        if (refreshToken.isNullOrBlank()) {
            auditService.log(
                type = AuditType.USER_SESSION_REFRESH_FAILED,
                properties = context.toProperties() + mapOf(
                    "reason" to "REFRESH_TOKEN_MISSING",
                )
            )

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val newAccessToken = service.refresh(refreshToken, context)

        response.addHeader(
            HttpHeaders.SET_COOKIE,
            service.createTokenCookie(
                "access_token",
                newAccessToken.token,
                accessTokenDuration,
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
    fun confirmEmail(
        @PathVariable verificationToken: String,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {

        service.confirmEmail(
            verificationToken,
            auditContext(httpRequest),
        )

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
    fun logout(
        response: HttpServletResponse,
        authentication: Authentication?,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Void> {

        val user = authentication
            ?.takeIf { it.isAuthenticated }
            ?.name
            ?.toLongOrNull()
            ?.let { userService.findById(it) }

        auditService.log(
            type = AuditType.USER_LOGOUT,
            actor = user,
            properties = auditContext(httpRequest).toProperties() + mapOf(
                "userId" to user?.id,
                "result" to "SUCCESS",
            )
        )

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

    private fun auditContext(request: HttpServletRequest) =
        AuthAuditContext(
            ipAddress = request.remoteAddr,
            userAgent = request.getHeader("User-Agent"),
            requestId = request.getHeader("X-Request-ID"),
        )
}