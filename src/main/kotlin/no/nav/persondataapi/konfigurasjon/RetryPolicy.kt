package no.nav.persondataapi.konfigurasjon

import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import org.slf4j.LoggerFactory
import reactor.util.retry.Retry
import java.net.ConnectException
import java.time.Duration
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.delay
import org.springframework.web.reactive.function.client.WebClientRequestException

object RetryPolicy {

    private val log = LoggerFactory.getLogger(RetryPolicy::class.java)

    /** Hvilke exceptions som regnes som retrybare */
    private val retryableExceptions: (Throwable) -> Boolean = { t ->
        val root = t.rootCause()
        when (root) {
            is TimeoutException,
            is ReadTimeoutException,
            is WriteTimeoutException,
            is ConnectException -> true
            else -> false
        }
    }

    /** Reactor Retry for WebClient */
    fun reactorRetrySpec(
        forsøk: Int = 3,
        kilde:String,
        initiellBackoff: Duration = Duration.ofMillis(200),
        maxBackoff: Duration = Duration.ofSeconds(2)
    ): Retry =
        Retry
            .backoff(forsøk.toLong(), initiellBackoff)
            .maxBackoff(maxBackoff)
            .filter(retryableExceptions)
            .doBeforeRetry { signal ->
                log.info(
                    "prøver kall mot ${kilde} på nytt " +
                            "(forsøk ${signal.totalRetries() + 1}) " +
                            "på grunn av ${signal.failure()::class.simpleName}: ${signal.failure().message}"
                )
            }
            .onRetryExhaustedThrow { _, signal -> signal.failure() }
            .transientErrors(true)

    /** Coroutine-basert retry for GraphQLWebClient (PDL) */
    suspend fun <T> coroutineRetry(
        forsøk: Int = 3,
        kilde:String,
        initialBackoffMs: Long = 200,
        block: suspend () -> T
    ): T {
        var lastError: Throwable? = null

        repeat(forsøk) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                lastError = e

                if (!retryableExceptions(e)) throw e
                /*
                *
                * Dette uttrykket lager exponential backoff — altså ventetid som øker eksponentielt for hver retry:
                * forsøk → 200 ms
                * forsøk → 400 ms
                * forsøk → 800 ms
                *
                * */

                val backoff = initialBackoffMs * (1L shl attempt)
                log.info("Prøver kall mot ${kilde} på nytt (forsøk ${attempt + 1}) etter ventetid ${backoff}ms på grunn av  ${e::class.simpleName}")

                delay(backoff)
            }
        }

        throw lastError ?: RuntimeException("Ukjent feil etter retry")
    }
}
fun Throwable.rootCause(): Throwable {
    var cause = this
    while (cause.cause != null && cause.cause !== cause) {
        cause = cause.cause!!
    }
    return cause
}
