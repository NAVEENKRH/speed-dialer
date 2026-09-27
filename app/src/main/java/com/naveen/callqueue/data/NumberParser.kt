package com.naveen.callqueue.data

/**
 * Pulls phone numbers (and, where present, an attached name) out of arbitrary pasted text:
 * Excel/Sheets copy-paste, a WhatsApp forward, plain notes, or a raw list of digits.
 *
 * Supported per-line shapes, mixed freely in the same paste:
 *   "Rahul: 98765 43210"
 *   "Priya 91234 56789"
 *   "+91 99887 76655"
 *   "9876543210"
 *   "919770930677"
 *   "Amit - 9988776655, 8877665544"   (multiple numbers on one line)
 */
object NumberParser {

    // A phone-like run of digits: allows spaces, hyphens, dots, parens and a leading '+',
    // total 10 to 13 digits once separators are stripped.
    private val PHONE_TOKEN = Regex("""\+?[\d][\d\s\-.()]{8,17}\d""")
    private val NON_DIGITS = Regex("""\D""")

    data class ParsedNumber(val name: String?, val number: String, val rawInput: String)

    fun parse(text: String): List<ParsedNumber> {
        val results = LinkedHashMap<String, ParsedNumber>() // key = normalized number, dedupe keeping first

        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val matches = PHONE_TOKEN.findAll(line).toList()
                    .mapNotNull { m -> normalize(m.value)?.let { m to it } }

                if (matches.isEmpty()) return@forEach

                if (matches.size == 1) {
                    val (match, normalized) = matches[0]
                    val name = extractName(line, match.range.first, match.range.last)
                    results.putIfAbsent(normalized, ParsedNumber(name, normalized, line))
                } else {
                    // Multiple numbers on one line (comma/space separated list) — no per-number name.
                    matches.forEach { (_, normalized) ->
                        results.putIfAbsent(normalized, ParsedNumber(null, normalized, line))
                    }
                }
            }

        return results.values.toList()
    }

    private fun extractName(line: String, matchStart: Int, matchEnd: Int): String? {
        val before = line.substring(0, matchStart).trim().trimEnd(':', '-', '–', '—').trim()
        val after = line.substring((matchEnd + 1).coerceAtMost(line.length)).trim().trimStart(':', '-').trim()
        val candidate = when {
            before.isNotEmpty() -> before
            after.isNotEmpty() -> after
            else -> null
        }
        if (candidate.isNullOrBlank()) return null
        // Reject candidates that are themselves mostly numeric (e.g. a second phone number, or stray punctuation).
        val digitCount = candidate.count { it.isDigit() }
        if (digitCount > candidate.length / 2) return null
        return candidate
    }

    /**
     * Strips separators, resolves a plausible national number (India-first, since this app
     * targets Indian mobile leads), and returns an E.164-ish string, or null if it doesn't
     * look like a real phone number at all.
     */
    private fun normalize(raw: String): String? {
        var digits = raw.replace(NON_DIGITS, "")
        if (digits.isEmpty()) return null

        if (digits.length == 11 && digits.startsWith("0")) digits = digits.substring(1)
        if (digits.length == 12 && digits.startsWith("91")) digits = digits.substring(2)
        if (digits.length == 13 && digits.startsWith("091")) digits = digits.substring(3)

        return when {
            digits.length == 10 -> "+91$digits"
            digits.length in 11..15 -> "+$digits"
            else -> null // too short/long to plausibly be a phone number
        }
    }

    /** Last-10-digit national number, used to fuzzy-match against the call log which may store numbers differently formatted. */
    fun last10(e164: String): String {
        val digits = e164.replace(NON_DIGITS, "")
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }
}
