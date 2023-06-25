package io.github.aecsocket.glossa.configurate

import io.github.aecsocket.glossa.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.BufferedReader
import java.io.StringReader
import java.util.*
import kotlin.test.Test

// TODO we need actual unit tests
class ConfigurateTest {
    fun printMessage(key: String, block: (key: String) -> Message) {
        val message = block(key)
        println("$key:")
        message.forEach { println("  $it") }
    }

    interface Messages {
        @MessageKey("hello_world")
        fun helloWorld(): Message

        @MessageKey("with_parameters")
        fun withParameters(
            @Placeholder("comp") comp: Component,
            @Placeholder("num") num: Int
        ): Message

        @MessageKey("multiline")
        fun multiline(): Message

        @MessageKey("message_list")
        fun messageList(): List<Message>

        @MessageKey("subsection.some_key")
        fun subsectionSomeKey(): Message
    }

    @Test
    fun testBasic() {
    }

    @Test
    fun testFromString() {
        val config = """
            substitutions:
              i_warn: "(!)"
            styles:
              info:
                color: gray
            translations:
              en-US:
                hello_world: "Hello World!"
                with_parameters: "Component: <comp> | Number: <i_warn> <info>{num, number}</info>"
                multiline: |-
                  <red>Line one
                  <blue>Line two
                message_list:
                 - "Message one"
                 - "Message two"
                 - |-
                   Line 1
                   Line 2
                subsection:
                  some_key: "This is a key"
        """.trimIndent()
        val locale = Locale.forLanguageTag("en-US")

        val glossa = glossaStandard(locale, InvalidMessageProvider.Default) {
            fromConfigLoader(
                YamlConfigurationLoader.builder()
                    .source { BufferedReader(StringReader(config)) }
                    .build()
            )
        }
        val messages = glossa.messageProxy<Messages>().forLocale(locale)

        printMessage("hello_world") { messages.helloWorld() }
        printMessage("with_parameters") {
            messages.withParameters(
                comp = text("Red", NamedTextColor.RED),
                num = 15
            )
        }
        printMessage("subsection.some_key") { messages.subsectionSomeKey() }
    }
}
