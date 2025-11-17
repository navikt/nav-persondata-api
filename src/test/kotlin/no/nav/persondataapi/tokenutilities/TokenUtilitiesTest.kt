package no.nav.persondataapi.tokenutilities

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class TokenUtilitiesTest {
	@Test
	fun `ingen navident skal returnere ukjent`() {
		val ident = hentNavIdent()
		Assertions.assertNotNull(ident)
		Assertions.assertEquals("ukjent", ident)
	}

	@Test
	fun `om navIdent er satt på MDC navident skal returneres`() {
		val eksempelNavIdent = "Z12345"
		MDC.put(NAV_IDENT, eksempelNavIdent)
		val ident = hentNavIdent()
		Assertions.assertNotNull(ident)
		Assertions.assertEquals(eksempelNavIdent, ident)
		MDC.remove(NAV_IDENT)
	}
}
