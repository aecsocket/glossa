package io.github.aecsocket.glossa.configurate

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import java.util.Locale

private const val ROOT = "root"

fun localeOf(text: String): Locale {
    return when (text) {
        ROOT -> Locale.ROOT
        else -> Locale.forLanguageTag(text)
    }
}

object LocaleSerializer : TypeSerializer<Locale> {
    override fun serialize(type: Type, obj: Locale?, node: ConfigurationNode) {
        if (obj == null) node.set(null)
        else {
            node.set(
                when (obj) {
                    Locale.ROOT -> ROOT
                    else -> obj.toLanguageTag()
                }
            )
        }
    }

    override fun deserialize(type: Type, node: ConfigurationNode): Locale {
        val text = node.string
            ?: throw SerializationException(node, type, "Must be expressed as string")
        return localeOf(text)
    }
}
