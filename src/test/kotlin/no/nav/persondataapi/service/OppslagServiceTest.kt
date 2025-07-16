package no.nav.persondataapi.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.nav.persondataapi.domain.UtbetalingRespons
import no.nav.persondataapi.service.dataproviders.GrunnlagsProvider
import no.nav.persondataapi.service.dataproviders.GrunnlagsType
import no.nav.persondataapi.service.dataproviders.GrunnlagsdelResultat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


@OptIn(ExperimentalCoroutinesApi::class)
class OppslagServiceTest {

    private lateinit var service: OppslagService

    @BeforeEach
    fun setup() {

    }

    @Test
    fun `skal returnere grunnlagsdata med utbetalinger`() = runTest {

    }
}
