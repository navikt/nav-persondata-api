package no.nav.persondataapi.rest.oppslag

/**
 * Annotation for marking fields that should be masked/anonymized under certain conditions.
 *
 * When applied to a String field, the field's value will be replaced with the specified
 * masked value if the masking precondition is met.
 *
 * @property maskertVerdi The value to use when masking this field. Defaults to "*******".
 *
 * Example usage:
 * ```
 * data class Person(
 *     @Maskert
 *     val navn: String,
 *
 *     @Maskert(maskertVerdi = "SKJULT")
 *     val adresse: String
 * )
 * ```
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Maskert(
    val maskertVerdi: String = "*******"
)
