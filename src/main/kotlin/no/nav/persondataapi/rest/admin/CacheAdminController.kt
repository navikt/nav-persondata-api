package no.nav.persondataapi.rest.admin

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.CacheAdminService
import no.nav.persondataapi.service.CacheFlushSummary
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/cache")
class CacheAdminController(
    private val cacheAdminService: CacheAdminService
) {

    private val logger = LoggerFactory.getLogger(CacheAdminController::class.java)

    @Protected
    @DeleteMapping
    fun flushCacher(
        @RequestBody(required = false) request: CacheFlushRequest?,
    ): ResponseEntity<CacheFlushSummary> {
        val oppsummering = if (request?.personIdent == null) {
            // TODO: Burde vi ha en slags form for validering av hvem som sender denne?
            logger.info("Flush request for all cacher mottatt")
            cacheAdminService.flushAlleCacher()
        } else {
            val personIdent = request.personIdent
            logger.info("Flush request for {} mottatt", personIdent)
            cacheAdminService.flushCacherForPersonIdent(personIdent)
        }

        return ResponseEntity.ok(oppsummering)
    }
}

data class CacheFlushRequest(
    val personIdent: PersonIdent?
)
