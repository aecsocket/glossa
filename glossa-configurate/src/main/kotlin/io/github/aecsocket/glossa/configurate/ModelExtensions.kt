package io.github.aecsocket.glossa.configurate

import io.github.aecsocket.glossa.*
import java.util.Locale
import net.kyori.adventure.serializer.configurate4.ConfigurateComponentSerializer
import net.kyori.adventure.text.format.Style
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.serialize.SerializationException

private fun validateKey(key: Any, node: ConfigurationNode): String {
  return try {
    validateGlossaKey(key.toString())
  } catch (ex: GlossaBuildException) {
    throw SerializationException(node, String::class.java, "Invalid key", ex)
  }
}

fun GlossaStandard.Model.substitutionsFromConfig(node: ConfigurationNode) {
  substitutions {
    node.childrenMap().forEach { (rawKey, child) ->
      val key = validateKey(rawKey, child)
      val substitution =
          child.string
              ?: throw SerializationException(child, String::class.java, "Requires substitution")
      miniMessageSubstitution(key, substitution)
    }
  }
}

fun GlossaStandard.Model.stylesFromConfig(node: ConfigurationNode) {
  styles {
    node.childrenMap().forEach { (rawKey, child) ->
      val key = validateKey(rawKey, child)
      val style =
          child.get<Style>()
              ?: throw SerializationException(child, Style::class.java, "Requires style value")
      style(key, style)
    }
  }
}

fun GlossaStandard.Model.translationsFromConfig(node: ConfigurationNode) {
  node.childrenMap().forEach { (localeText, translationRoot) ->
    val locale = localeOf(localeText.toString())

    fun walk(model: TranslationNode.Model, node: ConfigurationNode) {
      node.childrenMap().forEach { (rawKey, child) ->
        val key = validateKey(rawKey, child)
        when {
          child.isMap -> {
            model.section(key) { walk(this, child) }
          }
          child.isList -> {
            model.messageList(
                key,
                child.getList(String::class.java)
                    ?: throw SerializationException(
                        child,
                        String::class.java,
                        "Message list must be expressed as list of strings"))
          }
          else -> {
            model.message(
                key,
                child.string
                    ?: throw SerializationException(
                        child, String::class.java, "Message must be expressed as string"))
          }
        }
      }
    }

    translation(locale) { walk(this, translationRoot) }
  }
}

private const val SUBSTITUTIONS = "substitutions"
private const val STYLES = "styles"
private const val TRANSLATIONS = "translations"

fun GlossaStandard.Model.fromConfig(node: ConfigurationNode) {
  substitutionsFromConfig(node.node(SUBSTITUTIONS))
  stylesFromConfig(node.node(STYLES))
  translationsFromConfig(node.node(TRANSLATIONS))
}

private val configOptions =
    ConfigurationOptions.defaults().serializers { serializers ->
      serializers.registerAll(ConfigurateComponentSerializer.configurate().serializers())
      serializers.registerExact(Locale::class.java, LocaleSerializer)
    }

fun GlossaStandard.Model.fromConfigLoader(loader: ConfigurationLoader<*>) {
  fromConfig(loader.load(configOptions))
}
