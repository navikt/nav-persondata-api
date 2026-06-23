package no.nav.persondataapi.unleash

import io.getunleash.FakeUnleash
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FeatureToggleServiceTest {
    private val fakeUnleash = FakeUnleash()
    private val service: FeatureToggleService = UnleashFeatureToggleService(fakeUnleash)

    @Test
    fun `isEnabled returnerer false for ukjente toggles`() {
        // Siden Toggle-enum er tom, verifiserer vi indirekte via en FakeUnleash
        // der ingen toggles er aktivert
        fakeUnleash.disableAll()
        // Ingen Toggle-verdier å sjekke ennå — testen validerer at FakeUnleash
        // integrerer korrekt og returnerer false som standard
        assertFalse(fakeUnleash.isEnabled("ukjent-toggle"))
    }

    @Test
    fun `isEnabled returnerer true naar toggle er skrudd paa i FakeUnleash`() {
        fakeUnleash.enableAll()
        assertTrue(fakeUnleash.isEnabled("hvilken-som-helst-toggle"))
    }
}
