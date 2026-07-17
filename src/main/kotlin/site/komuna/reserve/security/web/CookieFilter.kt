package site.komuna.reserve.security.web

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter

class CsrfCookieFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val csrfToken = request.getAttribute("_csrf") as? CsrfToken
        println("CsrfCookieFilter CsrfToken from request: $csrfToken")

        csrfToken?.token //generate and save XSRF-TOKEN
        println("CsrfCookieFilter csrfToken.toString(): ${csrfToken?.token}")
        filterChain.doFilter(request, response)
    }
}