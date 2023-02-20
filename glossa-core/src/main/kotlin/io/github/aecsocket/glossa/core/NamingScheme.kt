package io.github.aecsocket.glossa.core

fun interface NamingScheme {
    fun coerce(words: List<String>): String

    companion object {
        val KebabCase = NamingScheme { words ->
            words.joinToString("-")
        }

        val SnakeCase = NamingScheme { words ->
            words.joinToString("_")
        }
    }
}

fun NamingScheme.coerceName(text: String) = coerce(wordsOfName(text))

fun wordsOfName(text: String): List<String> {
    val result = ArrayList<String>()
    val buffer = StringBuilder()

    fun swapBuffer() {
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
