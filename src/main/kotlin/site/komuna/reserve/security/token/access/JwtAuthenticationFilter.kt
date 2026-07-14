package site.komuna.reserve.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import site.komuna.reserve.security.token.access.AccessTokenService

@Component
class JwtAuthenticationFilter(
    private val service: AccessTokenService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        println("JwtAuthenticationFilter executed")

        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)

        println(authHeader)



        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)

        println("role = ${service.extractUserRole(token)}")
        println("userId = ${service.extractUserId(token)}")

        try {
            val userId = service.extractUserId(token)
            val role = service.extractUserRole(token)

            val authentication = UsernamePasswordAuthenticationToken(
                userId,
                null,
                listOf(SimpleGrantedAuthority("ROLE_$role"))
            )

            SecurityContextHolder.getContext().authentication = authentication

            println(SecurityContextHolder.getContext().authentication)

            println(SecurityContextHolder.getContext().authentication.authorities)

        } catch (_: Exception) {
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }
}