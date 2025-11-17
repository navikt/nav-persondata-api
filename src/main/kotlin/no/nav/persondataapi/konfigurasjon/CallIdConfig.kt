package no.nav.persondataapi.konfigurasjon

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.persondataapi.responstracing.LOGG_HEADER
import no.nav.persondataapi.tokenutilities.NAV_IDENT
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

object CallId {
	const val HEADER = "Nav-Call-Id"
	const val CTX_KEY = "navCallId"
}

@Component
@Order(Ordered.LOWEST_PRECEDENCE) // sikkert etter Spring Security / token-filteret
class NavCallIdServletFilter(
	private val tokenValidationContextHolder: TokenValidationContextHolder,
) : OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		chain: FilterChain,
	) {
		val navIdent =
			runCatching {
				val ctx = tokenValidationContextHolder.getTokenValidationContext()
				val token = ctx.firstValidToken // kan være null hvis ingen gyldige tokens
				token?.jwtTokenClaims?.get("NAVident")?.toString()
			}.getOrDefault("ukjent")
		val incoming = request.getHeader(CallId.HEADER) ?: UUID.randomUUID().toString()
		val loggRespons = request.getHeader(LOGG_HEADER)?.toBoolean()?.toString() ?: "false"
		MDC.put(LOGG_HEADER, loggRespons)
		MDC.put(CallId.HEADER, incoming)
		MDC.put(NAV_IDENT, navIdent)
		try {
			chain.doFilter(request, response)
		} finally {
			MDC.remove(CallId.HEADER)
			MDC.remove(NAV_IDENT)
			MDC.remove(LOGG_HEADER)
		}
	}
}
