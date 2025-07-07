package no.nav.persondataapi.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import no.nav.persondataapi.domain.UtbetalingRespons
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


@OptIn(ExperimentalCoroutinesApi::class)
class OppslagServiceTest {

    private lateinit var service: OppslagService

    @BeforeEach
    fun setup() {
        val utbetalingMockProvider = object : GrunnlagsProvider {
            override val type = GrunnlagsType.UTBETALINGER

            override suspend fun hent(fnr: String, saksbehandlerId: String): GrunnlagsdelResultat {
                return GrunnlagsdelResultat(
                    type = type,
                    data = UtbetalingRespons(utbetalinger = listOf()),
                    ok = true
                )
            }
        }

        service = OppslagService(listOf(utbetalingMockProvider))
    }

    @Test
    fun `skal returnere grunnlagsdata med utbetalinger`() = runTest {
        val result = service.hentGrunnlagsData("12345678901", "Z123456")
        assertEquals("12345678901", result.ident)
        assertTrue(result.utbetalingRespons!!.ok)
    }
}
