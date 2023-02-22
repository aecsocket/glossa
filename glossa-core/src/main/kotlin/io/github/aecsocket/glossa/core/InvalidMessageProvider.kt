package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component
import java.util.logging.Logger

/**
 * Determines a strategy for returning a message when no valid translation exists for a given key.
 */
interface InvalidMessageProvider {
    /**
     * Outputs the message key that was used in the operation.
     */
    object Default : InvalidMessageProvider {
        override fun missing(key: String): Message {
            return listOf(Component.text(key))
        }

        override fun invalidType(
            key: String,
            expected: MessageType
        ): Message {
            return listOf(Component.text(key))
        }
    }

    /**
     * Outputs the message key that was used in the operation, and issues a warning to the logger.
     */
    class DefaultLogging(private val logger: Logger) : InvalidMessageProvider {
        override fun missing(key: String): Message {
            logger.warning("Missing Glossa message for key '$key'")
            return listOf(Component.text(key))
        }

        override fun invalidType(
            key: String,
            expected: MessageType
        ): Message {
            logger.warning("Glossa message for key '$key' was expected to be ${expected.name}")
            return listOf(Component.text(key))
        }
    }

    /**
     * Strategy for handling a key which does not exist for any locale in the translator.
     */
    fun missing(key: String): Message

    /**
     * Strategy for handling a message creation call for the wrong type of message.
     */
    fun invalidType(key: String, expected: MessageType): Message
}
