package no.nav.persondataapi.rest

import kotlinx.coroutines.runBlocking

import no.nav.persondataapi.domain.GrunnlagsData
import no.nav.persondataapi.rest.domain.InvalidFnrException
import no.nav.persondataapi.rest.domain.OppslagBrukerRespons
import no.nav.persondataapi.service.OppslagService
import no.nav.persondataapi.service.RequestValidor
import no.nav.persondataapi.service.ResponsMappingService
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class OppslagBrukerController(
    private val oppslagService: OppslagService,
    private val mappingService: ResponsMappingService,
    private val requestValidor: RequestValidor
) {

    @PostMapping("/oppslag-bruker")
    @Protected
    fun userInfo(@RequestBody dto: OppslagBrukerRequest): OppslagBrukerRespons {
        if (!requestValidor.simpleDnrDnrValidation(dto.fnr)){
            throw InvalidFnrException("Fødselsnummeret '${dto.fnr}' er ikke gyldig")
        }
        return runBlocking {

            val grunnlag = oppslagService.hentGrunnlagsData(dto.fnr)
            mappingService.mapToOppslagBrukerResponse(grunnlag)
        }
    }

    @PostMapping("/oppslag-bruker-api")
    @Protected
    fun userInfoAPI(@RequestBody dto: OppslagBrukerRequest): GrunnlagsData {
        return runBlocking {

              val respons = oppslagService.hentGrunnlagsData(dto.fnr)
            respons

        }
    }
}

data class OppslagBrukerRequest(val fnr: String)
