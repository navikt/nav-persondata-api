package no.nav.persondataapi.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RequestValidatorTest {

    @Test
    fun `skal kunne validere fnr`() {
        val requestValidor = RequestValidor()
        Assertions.assertTrue(requestValidor.simpleDnrDnrValidation("05066323120"),"fnr validering feilet")
        Assertions.assertTrue(requestValidor.simpleDnrDnrValidation("67489147782"),"dnummer validering feilet")
        Assertions.assertFalse(requestValidor.simpleDnrDnrValidation("123456789012"))
    }
}