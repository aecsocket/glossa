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
    var defaultLocale: Locale,
    private val messages: Map<String, Map<Locale, MessageData>>,
    private val styles: Map<String, Style>,
    private val invalidMessageProvider: InvalidMessageProvider,
    private val miniMessage: MiniMessage = MiniMessage.miniMessage(),
    override var locale: Locale = defaultLocale,
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

    private fun messageData(locale: Locale, key: String): MessageData? {
        val forLocale = messages[key] ?: return null
        return forLocale[locale] ?: forLocale[defaultLocale] ?: forLocale[Locale.ROOT]
    }

    private fun buildTagResolver(args: MessageArgs) = TagResolver.builder().apply {
        // earlier is lower priority

        styles.forEach { (key, style) ->
            tag(key, Tag.styling { it.merge(style) })
        }

        args.replace.forEach { (key, value) ->
            tag(key, Tag.selfClosingInserting(value))
        }
    }.build()

    override fun message(locale: Locale, key: String, args: MessageArgs): Message {
        val data = messageData(locale, key) ?: return invalidMessageProvider.missing(key)
        if (data !is MessageData.Single) return invalidMessageProvider.invalidType(key, MessageType.SINGLE)

        val tagResolver = buildTagResolver(args)
        return data.entry.format(args.format).lines().map { line ->
            val text = line.format(args.format)
            miniMessage.deserialize(text, tagResolver).applyFallbackStyle(data.baseStyle)
        }
    }

    override fun messageList(locale: Locale, key: String, args: MessageArgs): List<Message> {
        val data = messageData(locale, key) ?: return listOf(invalidMessageProvider.missing(key))
        if (data !is MessageData.Multiple) return listOf(invalidMessageProvider.invalidType(key, MessageType.MULTIPLE))

        val tagResolver = buildTagResolver(args)

        return data.entries.map { entry ->
            entry.format(args.format).lines().map { line ->
                 val text = line.format(args.format)
                 miniMessage.deserialize(text, tagResolver).applyFallbackStyle(data.baseStyle)
            }
        }
    }

    interface Model {
        val styles: StylesModel
        fun styles(block: StylesModel.() -> Unit) =
            block(styles)

        fun translation(locale: Locale, block: TranslationNode.Model.() -> Unit = {})
    }

    interface StylesModel {
        fun style(key: String, style: Style)
    }
}

typealias TranslationPath = List<String>

fun TranslationPath.toGlossaKey() = joinToString(".")

sealed interface TranslationNode {
    val children: Map<String, TranslationNode>

    fun mergeFrom(other: TranslationNode)

    class Section : TranslationNode {
        override val children = HashMap<String, TranslationNode>()

        override fun mergeFrom(other: TranslationNode) {
            other.children.forEach { (key, child) ->
                if (child is Section) {
                    children[key]?.mergeFrom(child) ?: run { children[key] = child }
                } else {
                    children[key] = child
                }
            }
        }
    }

    data class Single(val entry: MessageFormat) : TranslationNode {
        override val children get() = emptyMap<String, TranslationNode>()

        override fun mergeFrom(other: TranslationNode) {}
    }

    data class Multiple(val entries: List<MessageFormat>) : TranslationNode {
        override val children get() = emptyMap<String, TranslationNode>()

        override fun mergeFrom(other: TranslationNode) {}
    }

    interface Model {
        fun section(key: String, block: Model.() -> Unit = {})

        fun message(key: String, value: String)

        fun messageList(key: String, value: List<String>)

        fun messageList(key: String, value: Iterable<String>) =
            messageList(key, value.toList())

        fun messageList(key: String, vararg value: String) =
            messageList(key, value.toList())
    }
}

class GlossaBuildException(
    val path: TranslationPath,
    val rawMessage: String? = null,
    cause: Throwable? = null
) : RuntimeException("${path.toGlossaKey()}: $rawMessage", cause)

private val KeyPattern = Regex("([a-z0-9_-])+")

private fun validate(key: String): String {
    if (!KeyPattern.matches(key))
        throw GlossaBuildException(listOf(key), "Invalid key '$key', must match ${KeyPattern.pattern}")
    return key
}

fun translationNodeSection(block: TranslationNode.Model.() -> Unit): TranslationNode.Section {
    val section = TranslationNode.Section()
    block(object : TranslationNode.Model {
        override fun section(key: String, block: TranslationNode.Model.() -> Unit) {
            validate(key)
            section.children[key] = try {
                translationNodeSection(block)
            } catch (ex: GlossaBuildException) {
                throw GlossaBuildException(listOf(key) + ex.path, ex.rawMessage, ex.cause)
            }
        }

        private fun formatOf(key: String, text: String) = try {
            MessageFormat(text)
        } catch (ex: IllegalArgumentException) {
            throw GlossaBuildException(listOf(key), "Could not construct message format", ex)
        }

        override fun message(key: String, value: String) {
            validate(key)
            section.children[key] = TranslationNode.Single(formatOf(key, value))
        }

        override fun messageList(key: String, value: List<String>) {
            validate(key)
            section.children[key] = TranslationNode.Multiple(value.map { formatOf(key, it) })
        }
    })
    return section
}

fun glossaStandard(
    defaultLocale: Locale,
    invalidMessageProvider: InvalidMessageProvider,
    miniMessage: MiniMessage = MiniMessage.miniMessage(),
    locale: Locale = defaultLocale,
    block: GlossaStandard.Model.() -> Unit
): GlossaStandard {
    val styles = HashMap<String, Style>()
    val translations = HashMap<Locale, TranslationNode.Section>()

    block(object : GlossaStandard.Model {
        override val styles = object : GlossaStandard.StylesModel {
            override fun style(key: String, style: Style) {
                styles[key] = style
            }
        }

        override fun translation(locale: Locale, block: TranslationNode.Model.() -> Unit) {
            val section = translationNodeSection(block)
            translations[locale]?.mergeFrom(section) ?: run { translations[locale] = section }
        }
    })

    val messages = HashMap<String, MutableMap<Locale, GlossaStandard.MessageData>>()
    translations.forEach { (locale, root) ->
        fun walk(section: TranslationNode.Section, path: List<String>, baseStyle: Style) {
            section.children.forEach children@ { (key, child) ->
                fun setForLocale(value: GlossaStandard.MessageData) {
                    val pathKey = (path + key).toGlossaKey()
                    messages.computeIfAbsent(pathKey) { HashMap() }[locale] = value
                }

                when (child) {
                    is TranslationNode.Section -> walk(child, path + key, baseStyle)
                    is TranslationNode.Single -> {
                        setForLocale(GlossaStandard.MessageData.Single(child.entry, baseStyle))
                    }
                    is TranslationNode.Multiple -> {
                        setForLocale(GlossaStandard.MessageData.Multiple(child.entries, baseStyle))
                    }
                }
            }
        }

        walk(root, emptyList(), Style.empty())
    }

    return GlossaStandard(
        defaultLocale,
        messages,
        styles,
        invalidMessageProvider,
        miniMessage,
        locale
    )
}
