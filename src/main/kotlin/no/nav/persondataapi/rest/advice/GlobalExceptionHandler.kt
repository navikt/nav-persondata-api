package no.nav.persondataapi.rest.advice

import no.nav.persondataapi.rest.domain.InvalidFnrException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.net.URI
import java.time.OffsetDateTime

data class ProblemDetail(
    val type: URI,
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(InvalidFnrException::class)
    fun handleInvalidFnr(ex: InvalidFnrException, request: jakarta.servlet.http.HttpServletRequest): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail(
            type = URI("https://example.com/probs/invalid-fnr"),
            title = "Ugyldig fødselsnummer",
            status = HttpStatus.BAD_REQUEST.value(),
            detail = ex.message ?: "Fødselsnummeret er ugyldig",
            instance = request.requestURI
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem)
    }
}
