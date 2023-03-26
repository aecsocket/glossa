package io.github.aecsocket.glossa

/**
 * Allows coercing one identifier to another identifier with a different name format.
 */
fun interface NamingScheme {
    /**
     * Converts a collection of words to an identifier with this instance's format.
     */
    fun coerce(words: Iterable<String>): String

    companion object {
        /**
         * Joins words with `-`.
         */
        val KebabCase = NamingScheme { words ->
            words.joinToString("-")
        }

        /**
         * Joins words with `_`.
         */
        val SnakeCase = NamingScheme { words ->
            words.joinToString("_")
        }
    }
}

/**
 * Converts a Java identifier name (in lowerCamelCase) to an identifier with this instance's format.
 */
fun NamingScheme.coerceName(text: String) = coerce(wordsOfName(text))

/**
 * Gets a list of words from a Java identifier name (in lowerCamelCase).
 *
 * For the conversion to be accurate:
 * - the identifier must be in lowercase
 * - word separators must be marked by a single capital letter at the start of the word
 * Examples:
 * - `health`
 * - `getHealth`
 * - `getApi`
 * - `getUuidStatus`
 */
fun wordsOfName(text: String): List<String> {
    val result = ArrayList<String>()
    val buffer = StringBuilder()

    fun swapBuffer() {
        if (buffer.isEmpty()) return
        result += buffer.toString()
        buffer.clear()
    }

    text.forEach { ch ->
        when {
            ch.isUpperCase() -> {
                swapBuffer()
                buffer.append(ch.lowercaseChar())
            }
            else -> buffer.append(ch)
        }
    }

    swapBuffer()
    return result
}
