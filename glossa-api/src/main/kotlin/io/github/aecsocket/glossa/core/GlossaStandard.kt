package io.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.*

/**
 * Default implementation of [Glossa].
 *
 * To create an instance, use the [glossaStandard] builder function.
 */
class GlossaStandard internal constructor(
    var defaultLocale: Locale,
    private val messages: Map<String, Map<Locale, MessageData>>,
    private val substitutions: Map<String, Component>,
    private val styles: Map<String, Style>,
    private val invalidMessageProvider: InvalidMessageProvider,
    private val miniMessage: MiniMessage = MiniMessage.miniMessage(),
    override var locale: Locale = defaultLocale,
) : Glossa {
    sealed interface MessageData {
        data class Single(
            val entry: MessageFormat
        ) : MessageData

        data class Multiple(
            val entries: List<MessageFormat>
        ) : MessageData
    }

    /**
     * Gets the number of total messages registered.
     */
    fun countMessages() = messages.size

    /**
     * Gets the number of unique locales registered.
     */
    fun countLocales() = messages
        .flatMap { (_, forLocale) -> forLocale.keys }
        .toSet().size

    /**
     * Gets the number of substitutions registered.
     */
    fun countSubstitutions() = substitutions.size

    /**
     * Gets the number of styles registered.
     */
    fun countStyles() = styles.size

    private fun messageData(locale: Locale, key: String): MessageData? {
        val forLocale = messages[key] ?: return null
        return forLocale[locale] ?: forLocale[defaultLocale] ?: forLocale[Locale.ROOT]
    }

    private fun buildTagResolver(args: MessageArgs) = TagResolver.builder().apply {
        // earlier is lower priority
        substitutions.forEach { (key, substitution) ->
            tag(key, Tag.selfClosingInserting(substitution))
        }

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
            miniMessage.deserialize(line, tagResolver)
        }
    }

    override fun messageList(locale: Locale, key: String, args: MessageArgs): List<Message> {
        val data = messageData(locale, key) ?: return listOf(invalidMessageProvider.missing(key))
        if (data !is MessageData.Multiple) return listOf(invalidMessageProvider.invalidType(key, MessageType.MULTIPLE))

        val tagResolver = buildTagResolver(args)

        return data.entries.map { entry ->
            entry.format(args.format).lines().map { line ->
                 val text = line.format(args.format)
                 miniMessage.deserialize(text, tagResolver)
            }
        }
    }

    interface Model {
        val substitutions: SubstitutionsModel

        val styles: StylesModel

        /**
         * Registers a set of translations for a specific locale.
         */
        fun translation(locale: Locale, block: TranslationNode.Model.() -> Unit = {})
    }

    interface SubstitutionsModel {
        /**
         * Registers a substitution, which is passed as a self-closing MiniMessage tag by [key].
         * @param key The key, which must conform to [validateGlossaKey].
         */
        fun substitution(key: String, substitution: Component)

        /**
         * Parses a component using this instance's [miniMessage] and registers it as a substitution,
         * which is passed as a self-closing MiniMessage tag by [key].
         * @param key The key, which must conform to [validateGlossaKey].
         */
        fun miniMessageSubstitution(key: String, substitution: String)
    }

    interface StylesModel {
        /**
         * Registers a style, which is passed as a MiniMessage open/closing tag by [key].
         * @param key The key, which must conform to [validateGlossaKey].
         */
        fun style(key: String, style: Style)
    }
}

fun GlossaStandard.Model.substitutions(block: GlossaStandard.SubstitutionsModel.() -> Unit) =
    block(substitutions)

fun GlossaStandard.Model.styles(block: GlossaStandard.StylesModel.() -> Unit) =
    block(styles)

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
        /**
         * Registers a subsection by [key].
         * @param key The key, which must conform to [validateGlossaKey].
         */
        fun section(key: String, block: Model.() -> Unit = {})

        /**
         * Registers a [MessageType.SINGLE] message by [key] under the current section.
         * @param key The key, which must conform to [validateGlossaKey].
         */
        fun message(key: String, value: String)

        /**
         * Registers a [MessageType.MULTIPLE] message by [key] under the current section.
         * @param key The key, which must conform to [validateGlossaKey].
         */
        fun messageList(key: String, value: List<String>)
    }
}

/**
 * Registers a [MessageType.MULTIPLE] message by [key] under the current section.
 * @param key The key, which must conform to [validateGlossaKey].
 */
fun TranslationNode.Model.messageList(key: String, value: Iterable<String>) =
    messageList(key, value.toList())

/**
 * Registers a [MessageType.MULTIPLE] message by [key] under the current section.
 * @param key The key, which must conform to [validateGlossaKey].
 */
fun TranslationNode.Model.messageList(key: String, vararg value: String) =
    messageList(key, value.toList())

class GlossaBuildException(
    val path: TranslationPath,
    val rawMessage: String? = null,
    cause: Throwable? = null
) : RuntimeException("${path.toGlossaKey()}: $rawMessage", cause)

private val keyPattern = Regex("([a-z0-9_])+")

/**
 * Validates a key for the [Glossa] engine. The key must match the pattern `([a-z0-9_])+`
 */
fun validateGlossaKey(key: String): String {
    if (!keyPattern.matches(key))
        throw GlossaBuildException(listOf(key), "Invalid key '$key', must match ${keyPattern.pattern}")
    return key
}

/**
 * Builds a [TranslationNode.Section] by a model.
 */
fun translationNodeSection(block: TranslationNode.Model.() -> Unit): TranslationNode.Section {
    val section = TranslationNode.Section()
    block(object : TranslationNode.Model {
        override fun section(key: String, block: TranslationNode.Model.() -> Unit) {
            validateGlossaKey(key)
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
            validateGlossaKey(key)
            section.children[key] = TranslationNode.Single(formatOf(key, value))
        }

        override fun messageList(key: String, value: List<String>) {
            validateGlossaKey(key)
            section.children[key] = TranslationNode.Multiple(value.map { formatOf(key, it) })
        }
    })
    return section
}

/**
 * Builds a [GlossaStandard] by a model.
 */
fun glossaStandard(
    defaultLocale: Locale,
    invalidMessageProvider: InvalidMessageProvider,
    miniMessage: MiniMessage = MiniMessage.miniMessage(),
    locale: Locale = defaultLocale,
    block: GlossaStandard.Model.() -> Unit
): GlossaStandard {
    val substitutions = HashMap<String, Component>()
    val styles = HashMap<String, Style>()
    val translations = HashMap<Locale, TranslationNode.Section>()

    block(object : GlossaStandard.Model {
        override val substitutions = object : GlossaStandard.SubstitutionsModel {
            override fun substitution(key: String, substitution: Component) {
                substitutions[key] = substitution
            }

            override fun miniMessageSubstitution(key: String, substitution: String) {
                substitutions[key] = miniMessage.deserialize(substitution)
            }
        }

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
        fun walk(section: TranslationNode.Section, path: List<String>) {
            section.children.forEach children@{ (key, child) ->
                fun setForLocale(value: GlossaStandard.MessageData) {
                    val pathKey = (path + key).toGlossaKey()
                    messages.computeIfAbsent(pathKey) { HashMap() }[locale] = value
                }

                when (child) {
                    is TranslationNode.Section -> walk(child, path + key)
                    is TranslationNode.Single -> {
                        setForLocale(GlossaStandard.MessageData.Single(child.entry))
                    }
                    is TranslationNode.Multiple -> {
                        setForLocale(GlossaStandard.MessageData.Multiple(child.entries))
                    }
                }
            }
        }

        walk(root, emptyList())
    }

    return GlossaStandard(
        defaultLocale,
        messages,
        substitutions,
        styles,
        invalidMessageProvider,
        miniMessage,
        locale
    )
}