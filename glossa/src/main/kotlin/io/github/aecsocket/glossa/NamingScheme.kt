package io.github.aecsocket.glossa

/**
 * Gets a list of words from a Java identifier name (in lowerCamelCase).
 *
 * For the conversion to be accurate:
 * - the identifier must be in lowercase
 * - word separators must be marked by a single capital letter at the start of the word Examples:
 * - `health`
 * - `getHealth`
 * - `getApi`
 * - `getUuidStatus`
 */
internal fun wordsOfName(name: String): List<String> {
  val result = ArrayList<String>()
  val buffer = StringBuilder()

  fun swapBuffer() {
    if (buffer.isEmpty()) return
    result += buffer.toString()
    buffer.clear()
  }

  name.forEach { ch ->
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

/** Coerces a Java identifier to `snake_case` via [wordsOfName]. */
internal fun coerceName(name: String): String {
  return wordsOfName(name).joinToString("_")
}
