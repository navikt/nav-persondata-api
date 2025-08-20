package no.nav.persondataapi.service.dataproviders



data class GrunnlagsdelResultat(
    val type: GrunnlagsType,
    val data: Any?,                // Typisk en DTO/domenemodell
    val ok: Boolean = true,
    val status:Int? = null,
    val feilmelding: String? = null
)

data class GrunnlagsKontekst(
    val fnr: String,
    val saksbehandlerId: String,
    val token: String
)
