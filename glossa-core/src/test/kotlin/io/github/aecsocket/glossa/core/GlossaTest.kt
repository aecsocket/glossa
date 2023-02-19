package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import java.util.Locale
import kotlin.test.Test

class GlossaTest {
    fun Message.print() = forEach { println(DefaultAnsiComponentRenderer.render(it)) }

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
        val reload: Message = glossa.message("command.reload") {
            replace("plugin-name", text("MyPlugin"))
        }
        reload.print()
        // [ Reloaded plugin MyPlugin ]

        val players0 = glossa.message("command.players") {
            format("players", 0)
        }
        players0.print()
        // [ There are no players online ]

        val players1 = glossa.message("command.players") {
            format("players", 1)
        }
        players1.print()
        // [ There is 1 player online ]

        val players10 = glossa.message("command.players") {
            format("players", 10)
        }
        players10.print()
        // [ There are 10 players online ]

        // Interpret `command.details` as a multiline message (type Message)
        // typealias Message = List<Component>
        val details = glossa.message("command.details") {
            replace("foo", text("foo"))
            replace("bar", text("bar"))
        }
        details.print()
        /* [
          Details:
           - Foo: foo
           - Bar: bar
         ] */

        // Interpret `command.splash_messages` as a list of messages (type List<Message>)
        val splashMessages = glossa.messageList("command.splash_messages")
        /* [
          [ Some funny message ]
          [ Another witty message ]
          [ A splash message, with multiple lines ]
         ] */

        val itemM9 = glossa.message("item.m9") {
            format("value", 100)
        }
        itemM9.print()
        val itemM4A1 = glossa.message("item.m4a1") {
            format("value", 500)
        }
        itemM4A1.print()
    }

    interface MyPluginMessages {
        @MessageKey("test")
        fun test(@Placeholder("value") value: Int, @Placeholder("comp") comp: Component): Message
        @MessageKey("test_lines")
        fun testLines(): List<Message>
    }

    @Test
    fun otherTest() {
        val messages: MessageProxy<MyPluginMessages> = glossa.messageProxy()

        messages.locale(Locale.ENGLISH).test(
            value = 1500,
            comp = text("Hello", NamedTextColor.RED)
        ).print()
        messages.locale(Locale.ENGLISH).testLines().forEach { it.print() }

//        val reload = myPluginMessages.command.reload()
//        val players0 = myPluginMessages.command.players(players = 0)
//        val players1 = myPluginMessages.command.players(players = 1)
//        val players10 = myPluginMessages.command.players(players = 10)
//
//        // `command.details` will have been defined as returning `Message`
//        val details = myPluginMessages.command.details(foo = text("foo"), bar = text("bar"))
//
//        // `command.splash_messages` will have been defined as returning `List<Message>`
//        val splashMessages = myPluginMessages.command.splashMessages()
//
//        // Entries under `item.*`:
//        // - max depth of 1 (no subkeys are allowed under `m9` or `m4a1`)
//        // - all accept the same parameters
//        // maybe this can be relaxed? idk
//        val itemM9 = myPluginMessages.item("m9", value = 100)
//        val itemM4A1 = myPluginMessages.item("m4a1", value = 500)
    }
}
