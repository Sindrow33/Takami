package moe.scenesearch.resolver

import kotlin.math.max
import kotlin.math.min

/** Jaro-Winkler по ядру плюс коэффициент Дайса по токенам, со штрафом за разный сезон. */
class TitleMatcher(
    private val seasonPenalty: Double = 0.25,
) {
    fun score(candidate: String, references: Collection<String>): Double {
        if (candidate.isBlank() || references.isEmpty()) return 0.0
        val c = TitleNormalizer.normalize(candidate)
        var best = 0.0
        for (ref in references) {
            if (ref.isBlank()) continue
            val s = score(c, TitleNormalizer.normalize(ref))
            if (s > best) best = s
        }
        return best
    }

    fun score(a: NormalizedTitle, b: NormalizedTitle): Double {
        if (a.core.isBlank() || b.core.isBlank()) return 0.0
        val base = max(jaroWinkler(a.core, b.core), dice(a.tokens, b.tokens))
        val sa = a.season ?: 1
        val sb = b.season ?: 1
        if (sa != sb) {
            val penalized = base - seasonPenalty
            return if (penalized < 0.0) 0.0 else penalized
        }
        return base
    }

    private fun dice(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        var inter = 0
        for (t in a) if (t in b) inter++
        return 2.0 * inter / (a.size + b.size)
    }

    private fun jaroWinkler(s1: String, s2: String): Double {
        val j = jaro(s1, s2)
        if (j < 0.7) return j
        var prefix = 0
        val limit = min(4, min(s1.length, s2.length))
        while (prefix < limit && s1[prefix] == s2[prefix]) prefix++
        return j + prefix * 0.1 * (1.0 - j)
    }

    private fun jaro(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val window = max(0, max(s1.length, s2.length) / 2 - 1)
        val m1 = BooleanArray(s1.length)
        val m2 = BooleanArray(s2.length)
        var matches = 0

        for (i in s1.indices) {
            val start = max(0, i - window)
            val end = min(i + window + 1, s2.length)
            var k = start
            while (k < end) {
                if (!m2[k] && s1[i] == s2[k]) {
                    m1[i] = true
                    m2[k] = true
                    matches++
                    break
                }
                k++
            }
        }
        if (matches == 0) return 0.0

        var transpositions = 0.0
        var k = 0
        for (i in s1.indices) {
            if (!m1[i]) continue
            while (k < m2.size && !m2[k]) k++
            if (k < s2.length && s1[i] != s2[k]) transpositions++
            k++
        }
        transpositions /= 2.0

        val m = matches.toDouble()
        return (m / s1.length + m / s2.length + (m - transpositions) / m) / 3.0
    }
}
