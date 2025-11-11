package no.nav.persondataapi.rest.oppslag

import no.nav.persondataapi.service.BegrunnetTilgangService
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/oppslag/begrunnet-tilgang")
class BegrunnetTilgangController(
    private val begrunnetTilgangService: BegrunnetTilgangService
) {

    @Protected
    @PostMapping
    fun loggBegrunnetTilgang(@RequestBody dto: BegrunnelseRequestDto): ResponseEntity<OppslagResponseDto<Unit>> {
        begrunnetTilgangService.loggBegrunnetTilgang(dto.ident,dto.begrunnelse, mangel = dto.mangel)
        return ResponseEntity.accepted().body(OppslagResponseDto(data = null))
    }
}
