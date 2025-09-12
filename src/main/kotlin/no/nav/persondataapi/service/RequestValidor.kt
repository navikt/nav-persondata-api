package no.nav.persondataapi.service

import org.springframework.stereotype.Component
import java.time.LocalDate


@Component
class RequestValidor() {
    private val K1 = intArrayOf(3,7,6,1,8,9,4,5,2)      // pos 1..9
    private val K2 = intArrayOf(5,4,3,2,7,6,5,4,3,2)    // pos 1..10
    private val fnrPattern = Regex("[0-9]{11}")
    fun validateFnrOrDnr(n: String): IdResult {
        if (!n.matches(Regex("\\d{11}"))) return IdResult(false, null, null)
        val d = n.map { it - '0' }

        val rawDay = d[0]*10 + d[1]
        val isDnr = rawDay in 41..71
        val day = if (isDnr) rawDay - 40 else rawDay
        val month = d[2]*10 + d[3]
        val yy = d[4]*10 + d[5]
        val individ = d[6]*100 + d[7]*10 + d[8]

        val year = resolveYear(yy, individ) ?: return IdResult(false, null, null)

        val birthDate = try { LocalDate.of(year, month, day) } catch (_: Exception) {
            return IdResult(false, null, null)
        }

        val k1 = kontroll(d, K1)
        if (k1 < 0 || k1 != d[9]) return IdResult(false, null, null)

        val k2 = kontroll(d, K2)
        if (k2 < 0 || k2 != d[10]) return IdResult(false, null, null)

        return IdResult(true, if (isDnr) IdType.DNR else IdType.FNR, birthDate)
    }

    fun simpleDnrDnrValidation(fnr: String): Boolean {
        return fnr.matches(fnrPattern)
    }

    private fun kontroll(d: List<Int>, w: IntArray): Int {
        val sum = w.indices.sumOf { d[it] * w[it] }
        val r = 11 - (sum % 11)
        return when (r) {
            11 -> 0
            10 -> -1   // ugyldig
            else -> r
        }
    }

    // Århundre-regler iht. Folkeregisteret
    private fun resolveYear(yy: Int, individ: Int): Int? = when {
        individ in 0..499 -> 1900 + yy
        individ in 500..749 && yy in 54..99 -> 1800 + yy
        individ in 900..999 && yy in 40..99 -> 1900 + yy
        individ in 500..999 && yy in 0..39  -> 2000 + yy
        else -> null
    }
}

enum class IdType { FNR, DNR }
data class IdResult(val valid: Boolean, val type: IdType?, val birthDate: LocalDate?)
