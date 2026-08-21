package io.github.jukerupup.mydictionary.data.parser

internal object MerriamWebsterMarkup {
    private val pairedTokens = setOf("it", "b", "inf", "sup", "phrase", "wi", "sc", "dx", "aq")

    fun render(source: String): String {
        val output = StringBuilder()
        val stack = ArrayDeque<String>()
        var cursor = 0
        while (cursor < source.length) {
            val opening = source.indexOf('{', cursor)
            if (opening < 0) {
                if (source.indexOf('}', cursor) >= 0) malformed()
                output.append(source, cursor, source.length)
                break
            }
            if (source.indexOf('}', cursor) in cursor until opening) malformed()
            output.append(source, cursor, opening)
            val closing = source.indexOf('}', opening + 1)
            if (closing < 0) malformed()
            val token = source.substring(opening + 1, closing)
            consume(token, source, closing + 1, output, stack)
            cursor = closing + 1
        }
        if (stack.isNotEmpty()) malformed()
        return output.toString()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([,.;:!?])"), "$1")
            .trim()
            .removePrefix(":")
            .trim()
    }

    private fun consume(
        token: String,
        source: String,
        nextCursor: Int,
        output: StringBuilder,
        stack: ArrayDeque<String>,
    ) {
        if (token.startsWith('/')) {
            val name = token.drop(1)
            if (stack.removeLastOrNull() != name) malformed()
            return
        }
        if ('|' in token) {
            val pieces = token.split('|')
            pieces.drop(1).firstOrNull { it.isNotBlank() }?.let(output::append)
            return
        }
        when (token) {
            "bc" -> output.append(": ")
            "ldquo", "rdquo" -> output.append('"')
            "apos" -> output.append('\'')
            "amp" -> output.append('&')
            "plus" -> output.append('+')
            "minus" -> output.append('-')
            "times" -> output.append('×')
            "p_br" -> output.append(' ')
            in pairedTokens -> stack.addLast(token)
            else -> {
                if (source.indexOf("{/$token}", nextCursor) < 0) malformed()
                stack.addLast(token)
            }
        }
    }

    private fun malformed(): Nothing = throw MalformedMarkupException()
}

internal class MalformedMarkupException : IllegalArgumentException()
