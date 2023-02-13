package io.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.minimessage.MiniMessage
import java.util.Locale
import java.util.logging.Logger

interface InvalidMessageProvider {
    object Default : InvalidMessageProvider {
        override fun missing(key: String): Message {
            return listOf(text(key))
        }

        override fun invalidType(
            key: String,
            expected: GlossaStandard.MessageType
        ): Message {
            return listOf(text(key))
        }
    }

    class DefaultLogging(private val logger: Logger) : InvalidMessageProvider {
        override fun missing(key: String): Message {
            logger.warning("Missing Glossa message for key '$key'")
            return listOf(text(key))
        }

        override fun invalidType(
            key: String,
            expected: GlossaStandard.MessageType
        ): Message {
            logger.warning("Glossa message for key '$key' was expected to be ${expected.name}")
            return listOf(text(key))
        }
    }

    fun missing(key: String): Message

    fun invalidType(key: String, expected: GlossaStandard.MessageType): Message
}

class GlossaStandard(
    private val messages: Map<String, Map<Locale, MessageData>>,
    private val invalidMessageProvider: InvalidMessageProvider,
    private val miniMessage: MiniMessage = MiniMessage.miniMessage(),
) : Glossa {
    enum class MessageType {
        SINGLE,
        MULTIPLE
    }

    sealed interface MessageData {
        data class Single(
            val lines: List<MessageFormat>
        ) : MessageData

        data class Multiple(
            val entries: List<List<MessageFormat>>
        ) : MessageData
    }

    private fun messageData(key: String) = messages[key]?.get(Locale.ROOT) // todo

    override fun message(key: String, args: GlossaArgs): Message {
        val data = messageData(key) ?: return invalidMessageProvider.missing(key)
        if (data !is MessageData.Single) return invalidMessageProvider.invalidType(key, MessageType.SINGLE)

        return data.lines.map { line ->
            val text = line.format(args.parse)

            text(text)
        }
    }

    override fun messageList(key: String, args: GlossaArgs): List<Message> {
        val data = messageData(key) ?: return listOf(invalidMessageProvider.missing(key))
        if (data !is MessageData.Multiple) return listOf(invalidMessageProvider.invalidType(key, MessageType.MULTIPLE))

        return data.entries.map { entry ->
             entry.map { line ->
                 val text = line.format(args.parse)

                 text(text)
             }
        }
    }
}
