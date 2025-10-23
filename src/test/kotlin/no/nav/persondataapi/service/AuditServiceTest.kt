package no.nav.persondataapi.service

import no.nav.persondataapi.rest.domene.PersonIdent
import org.junit.jupiter.api.Test

class AuditServiceTest {
    @Test
    internal fun `should create cef`() {
        val service = AuditService()
        AuditService().auditLookupGranted(PersonIdent(""),"ABCD@Nav.no")

    }

}