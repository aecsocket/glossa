package io.github.aecsocket.glossa

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import java.util.Locale

/**
 * The return type of [Glossa] message generation calls.
 */
typealias Message = List<Component>

/**
 * Joins all lines of this message to a single component with newline separators.
 */
fun Message.lines() = Component.join(JoinConfiguration.newlines(), this)

/**
 * Joins all lines of this message to a single component with no separators.
 */
fun Message.component() = Component.join(JoinConfiguration.noSeparators(), this)

/**
 * The type of message that a [Glossa] instance creates.
 */
enum class MessageType {
    /**
     * One message is created.
     */
    SINGLE,

    /**
     * A collection of messages is created.
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
 * If no translation exists for that key, what is returned is determined by the [InvalidMessageProvider].
 *
 * Generation methods return one or more [Message] objects. To convert this to a single component, the methods
 * [lines] or [component] can be used.
 *
 * To create an instance, use the [glossaStandard] builder function.
 */
interface Glossa {
    /**
     * The locale that this instance will generate messages for by default.
     */
    val locale: Locale

    /**
     * Creates a message for the provided locale.
     */
    fun message(locale: Locale, key: String, args: MessageArgs): Message

    /**
     * Creates a list of messages for the provided locale.
     */
    fun messageList(locale: Locale, key: String, args: MessageArgs): List<Message>
}

fun Glossa.message(key: String, args: MessageArgs) = message(locale, key, args)

fun Glossa.message(locale: Locale, key: String): Message =
    message(locale, key, MessageArgs.Empty)

fun Glossa.message(key: String) = message(locale, key)

fun Glossa.message(locale: Locale, key: String, args: MessageArgs.Model.() -> Unit): Message =
    message(locale, key, messageArgs(args))

fun Glossa.message(key: String, args: MessageArgs.Model.() -> Unit) = message(locale, key, args)

fun Glossa.messageList(key: String, args: MessageArgs) = messageList(locale, key, args)

fun Glossa.messageList(locale: Locale, key: String): List<Message> =
    messageList(locale, key, MessageArgs.Empty)

fun Glossa.messageList(key: String) = messageList(locale, key)

fun Glossa.messageList(locale: Locale, key: String, args: MessageArgs.Model.() -> Unit): List<Message> =
    messageList(locale, key, messageArgs(args))
fun Glossa.messageList(key: String, args: MessageArgs.Model.() -> Unit) = messageList(locale, key, args)

private class GlossaWithLocale(backing: Glossa, override val locale: Locale) : Glossa by backing

/**
 * Creates a decorated instance which wraps around this instance using a different default locale for method calls.
 */
fun Glossa.withLocale(locale: Locale): Glossa = GlossaWithLocale(this, locale)

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
        val Empty = MessageArgs(emptyMap(), emptyMap())
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
         */
        fun replace(key: String, value: Component)

        /**
         * Adds a key that is formatted by ICU.
         */
        fun format(key: String, value: Any)
    }
}

/**
 * Builds a [MessageArgs] object from a model.
 */
fun messageArgs(block: MessageArgs.Model.() -> Unit): MessageArgs {
    val replace = HashMap<String, Component>()
    val format = HashMap<String, Any>()

    block(object : MessageArgs.Model {
        override fun replace(key: String, value: Component) {
            replace[key] = value
        }

        override fun format(key: String, value: Any) {
            format[key] = value
        }
    })

    return MessageArgs(replace, format)
}
