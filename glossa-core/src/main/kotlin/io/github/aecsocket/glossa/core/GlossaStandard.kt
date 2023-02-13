package io.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
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
    private val styles: Map<String, Style>,
    private val invalidMessageProvider: InvalidMessageProvider,
    private val miniMessage: MiniMessage = MiniMessage.miniMessage(),
) : Glossa {
    enum class MessageType {
        SINGLE,
        MULTIPLE
    }

    sealed interface MessageData {
        data class Single(
            val entry: MessageFormat,
            val baseStyle: Style,
        ) : MessageData

        data class Multiple(
            val entries: List<MessageFormat>,
            val baseStyle: Style,
        ) : MessageData
    }

    private fun messageData(key: String) = messages[key]?.get(Locale.ROOT) // todo

    private fun buildTagResolver(args: GlossaArgs) = TagResolver.builder().apply {
        // earlier is lower priority

        styles.forEach { (key, style) ->
            tag(key, Tag.styling { it.merge(style) })
        }

        args.replace.forEach { (key, value) ->
            tag(key, Tag.selfClosingInserting(value))
        }
    }.build()

    override fun message(key: String, args: GlossaArgs): Message {
        val data = messageData(key) ?: return invalidMessageProvider.missing(key)
        if (data !is MessageData.Single) return invalidMessageProvider.invalidType(key, MessageType.SINGLE)

        val tagResolver = buildTagResolver(args)
        return data.entry.format(args.parse).lines().map { line ->
            val text = line.format(args.parse)
            miniMessage.deserialize(text, tagResolver).applyFallbackStyle(data.baseStyle)
        }
    }

    override fun messageList(key: String, args: GlossaArgs): List<Message> {
        val data = messageData(key) ?: return listOf(invalidMessageProvider.missing(key))
        if (data !is MessageData.Multiple) return listOf(invalidMessageProvider.invalidType(key, MessageType.MULTIPLE))

        val tagResolver = buildTagResolver(args)

        return data.entries.map { entry ->
            entry.format(args.parse).lines().map { line ->
                 val text = line.format(args.parse)
                 miniMessage.deserialize(text, tagResolver).applyFallbackStyle(data.baseStyle)
            }
        }
    }

    interface Model {
        val styles: StylesModel
        fun styles(block: StylesModel.() -> Unit) =
            block(styles)

        fun translation(locale: Locale, block: SectionModel.() -> Unit = {})
    }

    interface StylesModel {
        fun style(key: String, style: Style)
    }

    interface SectionModel {
        fun section(key: String, block: SectionModel.() -> Unit = {})

        fun message(key: String, value: String)

        fun messageList(key: String, value: List<String>)

        fun messageList(key: String, value: Iterable<String>) =
            messageList(key, value.toList())

        fun messageList(key: String, vararg value: String) =
            messageList(key, value.toList())
    }
}

private sealed interface MessageDefinition {
    data class Single(
        val entry: String
    ) : MessageDefinition

    data class Multiple(
        val entries: List<String>
    ) : MessageDefinition
}

fun glossaStandard(
    invalidMessageProvider: InvalidMessageProvider,
    miniMessage: MiniMessage = MiniMessage.miniMessage(),
    block: GlossaStandard.Model.() -> Unit
): GlossaStandard {
    val styles = HashMap<String, Style>()
    val translations = HashMap<Locale, MutableMap<String, MessageDefinition>>()

    block(object : GlossaStandard.Model {
        override val styles = object : GlossaStandard.StylesModel {
            override fun style(key: String, style: Style) {
                styles[key] = style
            }
        }

        override fun translation(locale: Locale, block: GlossaStandard.SectionModel.() -> Unit) {
        }
    })

    val messages = HashMap<String, MutableMap<Locale, GlossaStandard.MessageData>>()
    return GlossaStandard(messages, styles, invalidMessageProvider, miniMessage)
}
