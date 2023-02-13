package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration

typealias Message = List<Component>

fun Message.flatten() = Component.join(JoinConfiguration.newlines(), this)

interface Glossa {
    fun message(key: String, args: GlossaArgs): Message

    fun message(key: String): Message =
        message(key, GlossaArgs.Empty)

    fun message(key: String, args: GlossaArgsModel.() -> Unit): Message =
        message(key, glossaArgs(args))

    fun messageList(key: String, args: GlossaArgs): List<Message>

    fun messageList(key: String): List<Message> =
        messageList(key, GlossaArgs.Empty)

    fun messageList(key: String, args: GlossaArgsModel.() -> Unit): List<Message> =
        messageList(key, glossaArgs(args))
}

data class GlossaArgs(
    val replace: Map<String, Component>,
    val parse: Map<String, Any>,
) {
    companion object {
        val Empty = GlossaArgs(emptyMap(), emptyMap())
    }

    operator fun plus(other: GlossaArgs) = GlossaArgs(
        replace + other.replace,
        parse + other.parse,
    )
}

interface GlossaArgsModel {
    fun replace(key: String, value: Component)
    fun parse(key: String, value: Any)
}

fun glossaArgs(block: GlossaArgsModel.() -> Unit): GlossaArgs {
    val replace = HashMap<String, Component>()
    val parse = HashMap<String, Any>()

    block(object : GlossaArgsModel {
        override fun replace(key: String, value: Component) {
            replace[key] = value
        }

        override fun parse(key: String, value: Any) {
            parse[key] = value
        }
    })

    return GlossaArgs(replace, parse)
}
