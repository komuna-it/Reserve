package site.komuna.reserve.security.token.access

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import site.komuna.reserve.user.ban.BanService

@Component
class JwtAuthenticationFilter(
    private val service: AccessTokenService,
    private val banService: BanService,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        // checking bearer token - postman tests
        var token: String? = null
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7)
        } else {
           // check cookies for access_token
            val cookies = request.cookies
            if (cookies != null) {
                for (cookie in cookies) {
                    if (cookie.name == "access_token") {
                        token = cookie.value
                        break
                    }
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
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "User is banned until ${ban.banExpires}")
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