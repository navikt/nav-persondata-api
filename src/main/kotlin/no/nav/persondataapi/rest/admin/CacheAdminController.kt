package no.nav.persondataapi.rest.admin

import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.CacheAdminService
import no.nav.persondataapi.service.CacheFlushSummary
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/cache")
class CacheAdminController(
    private val cacheAdminService: CacheAdminService
) {

    private val logger = LoggerFactory.getLogger(CacheAdminController::class.java)

    @Protected
    @DeleteMapping
    fun flushCaches(
        @RequestParam(required = false) personIdent: String?
    ): ResponseEntity<CacheFlushSummary> {
        val summary = if (personIdent.isNullOrBlank()) {
            logger.info("Flush request for all caches mottatt")
            cacheAdminService.flushAllCaches()
        } else {
            val ident = PersonIdent(personIdent)
            logger.info("Flush request for {} mottatt", ident)
            cacheAdminService.flushCachesForPerson(ident)
        }

        return ResponseEntity.ok(summary)
    }
}
