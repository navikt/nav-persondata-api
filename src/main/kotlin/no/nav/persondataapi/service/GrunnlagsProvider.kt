package no.nav.persondataapi.service

interface GrunnlagsProvider {
    val type: GrunnlagsType

    suspend fun hent(fnr: String, saksbehandlerId: String): GrunnlagsdelResultat
}