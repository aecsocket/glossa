package io.github.aecsocket.glossa

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import java.util.Locale

/**
 * The return type of [Glossa] message generation calls.
 */
typealias Message = List<Component>

/**
 * Joins all lines of this message into a single component with newline separators.
 */
fun Message.lines() = Component.join(JoinConfiguration.newlines(), this)

/**
 * Joins all lines of this message into a single component with no separators.
 */
fun Message.oneLine() = Component.join(JoinConfiguration.noSeparators(), this)

/**
 * The type of message that a [Glossa] instance creates.
 */
enum class MessageType {
    /**
     * A single message.
     */
    SINGLE,

    /**
     * A collection of messages.
     */
    MULTIPLE
}

/**
 * The main interface for generating message translations.
 *
 * Generation is done by looking up the translation for a specific key. The lookup order by locale is:
 * - the locale provided in the method (optional)
 * - [locale]
 * - [Locale.ROOT]
 *
 * If no translation exists for that key, the return value is determined by the [InvalidMessageProvider].
 *
 * Generation methods return one or more [Message] objects. To convert this to a single component, the methods
 * [lines] or [oneLine] can be used.
 *
 * To create an instance, use the [glossaStandard] builder function.
 */
interface Glossa {
    /**
     * The locale that this instance will generate messages for by default.
     */
    val locale: Locale

    /**
     * Creates a message.
     * @param key The message key.
     * @param locale The locale to look up the translation for, or [Glossa.locale] if not provided.
     * @param args The arguments for templates in the translation, or [MessageArgs.empty] if not provided.
     */
    fun message(key: String, locale: Locale = this.locale, args: MessageArgs = MessageArgs.empty): Message

    /**
     * Creates a message.
     * @param key The message key.
     * @param locale The locale to look up the translation for, or [Glossa.locale] if not provided.
     * @param args A model for the arguments for templates in the translation.
     */
    fun message(key: String, locale: Locale = this.locale, args: MessageArgs.Model.() -> Unit) =
        message(key, locale, messageArgs(args))

    /**
     * Creates a list of messages.
     * @param key The message key.
     * @param locale The locale to look up the translation for, or [Glossa.locale] if not provided.
     * @param args The arguments for templates in the translation, or [MessageArgs.empty] if not provided.
     */
    fun messageList(key: String, locale: Locale = this.locale, args: MessageArgs = MessageArgs.empty): List<Message>

    /**
     * Creates a list of messages.
     * @param key The message key.
     * @param locale The locale to look up the translation for, or [Glossa.locale] if not provided.
     * @param args A model for the arguments for templates in the translation.
     */
    fun messageList(key: String, locale: Locale = this.locale, args: MessageArgs.Model.() -> Unit) =
        messageList(key, locale, messageArgs(args))

    /**
     * Creates a decorated instance which wraps around this instance, using a different default locale.
     * @param locale The new locale to translate with.
     */
    fun withLocale(locale: Locale): Glossa =
        GlossaWithLocale(this, locale)
}

private class GlossaWithLocale(
    backing: Glossa,
    override val locale: Locale,
) : Glossa by backing

/**
 * Arguments to be passed to [Glossa] message generation calls.
 * @param replace The keys that are replaced by components.
 * @param format The keys that are formatted by ICU.
 */
data class MessageArgs(
    val replace: Map<String, Component>,
    val format: Map<String, Any>,
) {
    companion object {
        /**
         * No arguments provided.
         */
        val empty = MessageArgs(emptyMap(), emptyMap())
    }

    /**
     * Merges this set of arguments with another, with the [other] taking priority.
     */
    operator fun plus(other: MessageArgs) = MessageArgs(
        replace + other.replace,
        format + other.format,
    )

    interface Model {
        /**
         * Adds a key that is replaced by a component.
         * @param key The template key.
         * @param value The value to replace with.
         */
        fun replace(key: String, value: Component)

        /**
         * Adds a key that is formatted by ICU.
         * @param key The template key.
         * @param value The value to format with.
         */
        fun format(key: String, value: Any)
    }
}

/**
 * Builds a [MessageArgs] object from a model.
 */
internal fun messageArgs(model: MessageArgs.Model.() -> Unit): MessageArgs {
    val replace = HashMap<String, Component>()
    val format = HashMap<String, Any>()

    model(object : MessageArgs.Model {
        override fun replace(key: String, value: Component) {
            replace[key] = value
        }

        override fun format(key: String, value: Any) {
            format[key] = value
        }
    })

    return MessageArgs(replace, format)
}
