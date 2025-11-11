package no.nav.persondataapi.rest.oppslag

import io.mockk.mockk
import io.mockk.verify
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.BegrunnetTilgangService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class AuditControllerTest {

    private val auditLogService: BegrunnetTilgangService = mockk(relaxed = true)
    private val controller = BegrunnetTilgangController(auditLogService)

    @Test
    fun `skal logge audit og returnere accepted`() {
        val request = BegrunnelseRequestDto(PersonIdent("12345678901"),"begrunnelse")

        val response = controller.loggBegrunnetYilgang(request,)

        verify { auditLogService.loggBegrunnetTilgang(request.ident,request.begrunnelse) }
        assertEquals(HttpStatus.ACCEPTED, response.statusCode)
    }
}
