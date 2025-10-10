
import no.nav.persondataapi.konfigurasjon.JsonUtils
import no.nav.persondataapi.integrasjon.utbetaling.dto.Utbetaling
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

class UtbetalingResponsMapperTest {

    @Test
    fun kanLeseResponsFraSOKOSUtbetalingsAPI() {


        val jsonString = readJsonFromFile("27525728205.json")
        val utbetalinger: List<Utbetaling> = JsonUtils.fromJson(jsonString)

        Assertions.assertNotNull(utbetalinger)
    }

    private fun readJsonFromFile(filename: String): String {
        val resource = ClassPathResource(filename)
        val inputStream = resource.inputStream
        return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8)
    }
}
