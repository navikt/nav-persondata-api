package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking
import no.nav.persondataapi.aareg.client.Ident
import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.rest.domain.OppslagBrukerRespons
import no.nav.persondataapi.service.OppslagService
import no.nav.persondataapi.service.ResponsMappingService
import no.nav.persondataapi.service.TokenService

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.context.TokenValidationContextHolder


import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController


@RestController
class OppslagBrukerController(
    private val oppslagService: OppslagService,
    private val mappingService: ResponsMappingService
) {

    @PostMapping("/oppslag-bruker")
    @Protected
    fun userInfo(@RequestBody dto: OppslagBrukerRequest): OppslagBrukerRespons {
        return runBlocking {

            val grunnlag = oppslagService.hentGrunnlagsData(dto.fnr)
            mappingService.mapToMOppslagBrukerResponse(grunnlag)
        }
    }
    @GetMapping("/oppslag-bruker-api")
    @Protected
    fun userInfoAPI(@RequestHeader("fnr") fnr: String): GrunnlagsData {
        return runBlocking {

              val respons = oppslagService.hentGrunnlagsData(fnr)
            respons

        }
    }
}

data class OppslagBrukerRequest(val fnr: String)
