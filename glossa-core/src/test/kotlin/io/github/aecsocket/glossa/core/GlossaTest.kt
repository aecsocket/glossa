package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component.text
import java.util.Locale
import kotlin.test.Test

class GlossaTest {
    @Test
    fun testGlossa() {
        val glossa = glossaStandard {
            translation(Locale.ENGLISH) {
                section("command") {
                    message("reload", "Reloaded plugin <plugin-name>")
                    message("players", """
                        There {players, plural,
                            =0 {are no players},
                            =1 {is # player},
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
            }
        }

        val reload: Message = glossa.message("command.reload") {
            replace("plugin-name", text("MyPlugin"))
        }
        // [ Reloaded plugin MyPlugin ]

        val players0 = glossa.message("command.players") {
            parse("players", 0)
        }
        // [ There are no players online ]

        val players1 = glossa.message("command.players") {
            parse("players", 1)
        }
        // [ There is 1 player online ]

        val players10 = glossa.message("command.players") {
            parse("players", 10)
        }
        // [ There are 10 players online ]

        // Interpret `command.details` as a multiline message (type Message)
        // typealias Message = List<Component>
        val details = glossa.message("command.details") {
            replace("foo", text("foo"))
            replace("bar", text("bar"))
        }
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
            parse("value", 100)
        }
        val itemM4A1 = glossa.message("item.m4a1") {
            parse("value", 500)
        }
    }

    interface MyPluginMessages {
        interface Command {
            fun reload(): Message

            fun players(players: Int): Message
        }

        val command: Command
    }

//    @Test
//    fun otherTest() {
//        val myPluginMessages: MyPluginMessages = glossa.createMessagesOrSomethingIdk(MyPluginMessages::class /* which defines the interface */)
//
//        val reload: Message = myPluginMessages.command.reload()
//        val players0 = myPluginMessages.command.players(players = 0)
//        val players1 = myPluginMessages.command.players(players = 1)
//        val players10 = myPluginMessages.command.players(players = 10)
//
//        // `command.details` will have been defined as returning `Message`
//        val details = myPluginMessages.command.details(foo = Component.text("foo"), bar = Component.text("bar"))
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
//    }
}
