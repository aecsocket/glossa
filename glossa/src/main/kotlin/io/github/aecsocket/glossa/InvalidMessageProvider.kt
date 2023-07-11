package io.github.aecsocket.glossa

import java.util.logging.Logger
import net.kyori.adventure.text.Component

/**
 * Determines a strategy for returning a message when no valid translation exists for a given key.
 */
interface InvalidMessageProvider {
  /** Outputs the message key that was used in the operation. */
  object Default : InvalidMessageProvider {
    override fun missing(key: String): Message {
      return listOf(Component.text(key))
    }

    override fun invalidType(key: String, expected: MessageType): Message {
      return listOf(Component.text(key))
    }
  }

  /**
   * Outputs the message key that was used in the operation, and passes a warning message to the log
   * consumer.
   */
  open class Logging(private val logger: (String) -> Unit) : InvalidMessageProvider {
    override fun missing(key: String): Message {
      logger("Missing Glossa message for key '$key'")
      return listOf(Component.text(key))
    }

    override fun invalidType(key: String, expected: MessageType): Message {
      logger("Glossa message for key '$key' was expected to be ${expected.name}")
      return listOf(Component.text(key))
    }
  }

  /**
   * Outputs the message key that was used in the operation, and issues a warning to the [Logger]
   * instance.
   */
  open class JavaLogging(private val logger: Logger) : Logging({ logger.warning(it) })

  /** Strategy for handling a key which does not exist for any locale in the translator. */
  fun missing(key: String): Message

  /** Strategy for handling a message creation call for the wrong type of message. */
  fun invalidType(key: String, expected: MessageType): Message
}
