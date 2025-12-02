package no.nav.persondataapi.konfigurasjon

import io.netty.handler.timeout.ReadTimeoutException
import io.netty.handler.timeout.WriteTimeoutException
import org.slf4j.LoggerFactory
import reactor.util.retry.Retry
import java.net.ConnectException
import java.time.Duration
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.delay

object RetryPolicy {

    private val log = LoggerFactory.getLogger(RetryPolicy::class.java)

    /** Hvilke exceptions som regnes som retrybare */
    val retryableExceptions: (Throwable) -> Boolean = { t ->
        t is TimeoutException ||
                t is ReadTimeoutException ||
                t is WriteTimeoutException ||
                t is ConnectException ||
                t.message?.contains("Serverfeil") == true
    }

    /** Reactor Retry for WebClient */
    fun reactorRetrySpec(
        attempts: Long = 3,
        kilde:String = "underliggende system",
        initialBackoff: Duration = Duration.ofMillis(200),
        maxBackoff: Duration = Duration.ofSeconds(2)
    ): Retry =
        Retry
            .backoff(attempts, initialBackoff)
            .maxBackoff(maxBackoff)
            .filter(retryableExceptions)
            .doBeforeRetry { signal ->
                log.info(
                    "prøver kall mot ${kilde }på nytt " +
                            "(forsøk ${signal.totalRetries() + 1}) " +
                            "på grunn av ${signal.failure()::class.simpleName}: ${signal.failure().message}"
                )
            }
            .onRetryExhaustedThrow { _, signal -> signal.failure() }
            .transientErrors(true)

    /** Coroutine-basert retry for GraphQLWebClient (PDL) */
    suspend fun <T> coroutineRetry(
        attempts: Int = 3,
        kilde:String = "underliggende system",
        initialBackoffMs: Long = 200,
        block: suspend () -> T
    ): T {
        var lastError: Throwable? = null

        repeat(attempts) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                lastError = e

                if (!retryableExceptions(e)) throw e

                val backoff = initialBackoffMs * (1L shl attempt)
                log.info("prøver kall mot ${kilde }på nytt  (forsøk ${attempt + 1}) etter ventetid ${backoff}ms på grunn av  ${e::class.simpleName}")

                delay(backoff)
            }
        }

        throw lastError ?: RuntimeException("Ukjent feil etter retry")
    }
}
