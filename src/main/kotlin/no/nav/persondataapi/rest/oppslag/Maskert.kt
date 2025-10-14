package no.nav.persondataapi.rest.oppslag

/**
 * Annotasjon for å markere felt som skal maskeres i gitte situasjoner.
 *
 * Når denne annotasjonen legges til et String-felt, så sier vi at vi vil maskere verdien av feltet.
 *
 * @property maskertVerdi Verdien som skal brukes når feltet maskeres. Standard er "*******".
 *
 * @example
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
