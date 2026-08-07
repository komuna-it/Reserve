package site.komuna.reserve.security.token.access

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import site.komuna.reserve.common.error.ErrorResponse
import site.komuna.reserve.common.error.ErrorType
import site.komuna.reserve.user.ban.BanService
import java.time.OffsetDateTime

@Component
class JwtAuthenticationFilter(
    private val service: AccessTokenService,
    private val banService: BanService,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        var token: String? = null

        val cookies = request.cookies
        if (cookies != null) {
            for (cookie in cookies) {
                if (cookie.name == "access_token") {
                    token = cookie.value
                    break
                }
            }
        }

        // no token found - filter by spring security
        if (token.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        // checking ban
        try {
            val userId = service.extractUserId(token)
            val role = service.extractUserRole(token)

            val ban = banService.isUserBanned(userId)

            if (ban != null) {
                logger.warn("User $userId is banned until ${ban.banExpires}")

                val errorResponse = ErrorResponse().apply {
                    this.errorType = ErrorType.USER_BANNED
                    this.message = "User is banned until ${ban.banExpires}"
                    this.bannedUntil = objectMapper.writeValueAsString(ban.banExpires) as OffsetDateTime?
                }

                response.status = HttpServletResponse.SC_FORBIDDEN
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.characterEncoding = "UTF-8"

                objectMapper.writeValue(response.writer, errorResponse)
                return
            }

            val authentication = UsernamePasswordAuthenticationToken(
                userId,
                null,
                listOf(SimpleGrantedAuthority("ROLE_$role"))
            )

            SecurityContextHolder.getContext().authentication = authentication

        } catch (e: Exception) {
            e.printStackTrace()
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }
}