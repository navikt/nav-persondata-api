package no.nav.persondataapi.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC

import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter


import java.util.UUID

object CallId {
    const val HEADER = "Nav-Call-Id"
    const val CTX_KEY = "navCallId"
}

@Component
@Order(0) // tidlig i kjeden
class NavCallIdServletFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val incoming = request.getHeader(CallId.HEADER) ?: UUID.randomUUID().toString()
        MDC.put(CallId.HEADER, incoming)
        try {
            chain.doFilter(request, response)
        } finally {
            MDC.remove(CallId.HEADER)
        }
    }
}
