package no.nav.persondataapi.unleash

import io.getunleash.Unleash

interface FeatureToggleService {
    fun isEnabled(toggle: Toggle): Boolean
}

class UnleashFeatureToggleService(
    private val unleash: Unleash,
) : FeatureToggleService {
    override fun isEnabled(toggle: Toggle): Boolean = unleash.isEnabled(toggle.toggleName)
}
