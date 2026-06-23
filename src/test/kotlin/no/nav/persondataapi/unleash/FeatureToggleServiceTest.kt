package no.nav.persondataapi.unleash

import io.getunleash.FakeUnleash
import no.nav.persondataapi.unleash.Toggle.WATSON_SOK_V_1_2
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureToggleServiceTest {
    private val fakeUnleash = FakeUnleash()
    private val service: FeatureToggleService = UnleashFeatureToggleService(fakeUnleash)

    @Test
    fun `isEnabled returnerer false når toggle er av`() {
        fakeUnleash.disableAll()
        assertFalse(service.isEnabled(WATSON_SOK_V_1_2))
    }

    @Test
    fun `isEnabled returnerer true når toggle er på`() {
        fakeUnleash.enable(WATSON_SOK_V_1_2.toggleName)
        assertTrue(service.isEnabled(WATSON_SOK_V_1_2))
    }
}
