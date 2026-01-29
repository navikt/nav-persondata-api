package no.nav.persondataapi.rest.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.persondataapi.rest.domene.PersonIdent
import no.nav.persondataapi.service.CacheAdminService
import no.nav.persondataapi.service.CacheFlushOppsummering
import no.nav.security.token.support.core.api.Protected
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/cache")
@Tag(name = "Admin", description = "Administrasjonsendepunkter for cache-håndtering")
class CacheAdminController(
    private val cacheAdminService: CacheAdminService
) {

    private val logger = LoggerFactory.getLogger(CacheAdminController::class.java)

    @Protected
    @DeleteMapping
    @Operation(
        summary = "Tøm cache",
        description = "Tømmer cache for en spesifikk person eller alle cacher hvis ingen personident er oppgitt"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        content = [Content(
            examples = [
                ExampleObject(
                    name = "Tøm for person",
                    value = """{"personIdent": "12345678901"}"""
                ),
                ExampleObject(
                    name = "Tøm alle cacher",
                    value = """{}"""
                )
            ]
        )]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Cache tømt",
                content = [Content(
                    schema = Schema(implementation = CacheFlushOppsummering::class),
                    examples = [ExampleObject(
                        name = "Vellykket respons",
                        value = """{"antallTømt": 5, "cacher": ["pdl-person", "aareg-arbeidsforhold"]}"""
                    )]
                )]
            )
        ]
    )
    fun flushCacher(
        @RequestBody(required = false) request: CacheFlushRequest?,
    ): ResponseEntity<CacheFlushOppsummering> {
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
