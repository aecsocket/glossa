package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import java.util.Locale

typealias Message = List<Component>

fun Message.line() = Component.join(JoinConfiguration.newlines(), this)

interface Glossa {
    val locale: Locale

    fun message(locale: Locale, key: String, args: MessageArgs): Message
    fun message(key: String, args: MessageArgs) = message(locale, key, args)

    fun message(locale: Locale, key: String): Message =
        message(locale, key, MessageArgs.Empty)
    fun message(key: String) = message(locale, key)

    fun message(locale: Locale, key: String, args: MessageArgs.Model.() -> Unit): Message =
        message(locale, key, messageArgs(args))
    fun message(key: String, args: MessageArgs.Model.() -> Unit) = message(locale, key, args)

    fun messageList(locale: Locale, key: String, args: MessageArgs): List<Message>
    fun messageList(key: String, args: MessageArgs) = messageList(locale, key, args)

    fun messageList(locale: Locale, key: String): List<Message> =
        messageList(locale, key, MessageArgs.Empty)
    fun messageList(key: String) = messageList(locale, key)

    fun messageList(locale: Locale, key: String, args: MessageArgs.Model.() -> Unit): List<Message> =
        messageList(locale, key, messageArgs(args))
    fun messageList(key: String, args: MessageArgs.Model.() -> Unit) = messageList(locale, key, args)
}

private class GlossaWithLocale(backing: Glossa, override val locale: Locale) : Glossa by backing

fun Glossa.withLocale(locale: Locale): Glossa = GlossaWithLocale(this, locale)

data class MessageArgs(
    val replace: Map<String, Component>,
    val format: Map<String, Any>,
) {
    companion object {
        val Empty = MessageArgs(emptyMap(), emptyMap())
    }

    operator fun plus(other: MessageArgs) = MessageArgs(
        replace + other.replace,
        format + other.format,
    )

    interface Model {
        fun replace(key: String, value: Component)
        fun format(key: String, value: Any)
    }
}

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
