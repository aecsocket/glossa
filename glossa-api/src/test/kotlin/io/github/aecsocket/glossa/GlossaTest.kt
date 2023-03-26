package io.github.aecsocket.glossa

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import java.util.Locale
import java.util.logging.Logger
import kotlin.test.Test

private val testAnsiComponentRenderer = AnsiComponentRenderer(ColorLevel.Indexed16)

fun printMessage(key: String, block: (key: String) -> Message) {
    val message = block(key)
    println("$key:")
    message.forEach { println("  ${testAnsiComponentRenderer.render(it)}") }
}

fun printMessageList(key: String, block: (key: String) -> List<Message>) {
    val messageList = block(key)
    println("$key:")
    messageList.forEachIndexed { idx, message ->
        val prefix = "  ${idx+1}) "
        val indent = " ".repeat(prefix.length)
        message.forEachIndexed { lineIdx, line ->
            println((if (lineIdx == 0) prefix else indent) + testAnsiComponentRenderer.render(line))
        }
    }
}

class GlossaTest {
    val glossa = glossaStandard(
        defaultLocale = Locale.ENGLISH,
        invalidMessageProvider = InvalidMessageProvider.Default,
    ) {
        translation(Locale.ENGLISH) {
            section("command") {
                message("reload", "Reloaded plugin <plugin-name>")
                message("players", """
                        There {players, plural,
                            zero {are no players}
                            one {is # player}
                            other {are # players}
                        } online.
                    """.trimIndent())
                message("details", """
                        Details:
                        - Foo: <foo>
                        - Bar: <bar>
                    """.trimIndent())
                messageList("splash_messages",
                    "Some funny message",
                    "Another witty message",
                    """
                        A splash message
                        with multiple lines
                    """.trimIndent())
            }

            section("item") {
                message("m9", "Beretta M9 (cost: {value})")
                message("m4a1", "Colt M4A1 (cost: {value})")
            }

            message("test", "Hello world! Val: {value, number}, component: <comp>")
            messageList("test_lines",
                "Message one",
                "Message two")
        }
    }

    @Test
    fun testGlossa() {
//        val reload: Message = glossa.message("command.reload") {
//            replace("plugin-name", text("MyPlugin"))
//        }
//        reload.print()
//        // [ Reloaded plugin MyPlugin ]
//
//        val players0 = glossa.message("command.players") {
//            format("players", 0)
//        }
//        players0.print()
//        // [ There are no players online ]
//
//        val players1 = glossa.message("command.players") {
//            format("players", 1)
//        }
//        players1.print()
//        // [ There is 1 player online ]
//
//        val players10 = glossa.message("command.players") {
//            format("players", 10)
//        }
//        players10.print()
//        // [ There are 10 players online ]
//
//        // Interpret `command.details` as a multiline message (type Message)
//        // typealias Message = List<Component>
//        val details = glossa.message("command.details") {
//            replace("foo", text("foo"))
//            replace("bar", text("bar"))
//        }
//        details.print()
//        /* [
//          Details:
//           - Foo: foo
//           - Bar: bar
//         ] */
//
//        // Interpret `command.splash_messages` as a list of messages (type List<Message>)
//        val splashMessages = glossa.messageList("command.splash_messages")
//        /* [
//          [ Some funny message ]
//          [ Another witty message ]
//          [ A splash message, with multiple lines ]
//         ] */
//
//        val itemM9 = glossa.message("item.m9") {
//            format("value", 100)
//        }
//        itemM9.print()
//        val itemM4A1 = glossa.message("item.m4a1") {
//            format("value", 500)
//        }
//        itemM4A1.print()
    }

    interface MyPluginMessages {
        @MessageKey
        fun helloWorld(): Message

        @MessageKey
        fun withParameters(
            component: Component,
            number: Int
        ): Message

        @MessageKey
        fun testMessageList(): List<Message>

        @SectionKey
        val subsection: Subsection
        interface Subsection {
            @MessageKey
            fun aSubKey(): Message
        }
    }

    @Test
    fun testMessages() {
        val logger = Logger.getAnonymousLogger()
        val english = Locale.ENGLISH
        val glossa = glossaStandard(english, InvalidMessageProvider.DefaultLogging(logger)) {
            translation(english) {
                message("hello_world", "Hello world!")
                message("with_parameters", "Component: <component> | Number: {number}")
                messageList("test_message_list",
                    "Message one",
                    "Message two",
                    """
                        Multiline message
                        with a second line
                    """.trimIndent())
                section("subsection") {
                    message("a_sub_key", "Some sub key")
                }
            }
        }
        val messages = glossa.messageProxy<MyPluginMessages>().default

        printMessage("hello_world") {
            messages.helloWorld()
        }

        printMessage("with_parameters") {
            messages.withParameters(
                component = text("Hello", NamedTextColor.RED),
                number = 15
            )
        }

        printMessageList("test_message_list") {
            messages.testMessageList()
        }

        printMessage("subsection.a_sub_key") {
            messages.subsection.aSubKey()
        }
    }
}
