package no.nav.persondataapi.service.dataproviders

import no.nav.persondataapi.service.dataproviders.GrunnlagsType
import no.nav.persondataapi.service.dataproviders.GrunnlagsdelResultat

interface GrunnlagsProvider {
    val type: GrunnlagsType

    suspend fun hent(fnr: String, saksbehandlerId: String): GrunnlagsdelResultat
}