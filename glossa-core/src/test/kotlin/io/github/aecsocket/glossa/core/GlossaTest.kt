package io.github.aecsocket.glossa.core

import kotlin.test.Test

class GlossaTest {
    lateinit var glossa: Glossa
    /*
    command.reload: "Reloaded plugin <plugin-name>"
    command.players: "There {players, plural, 0 { are no players } one { is # player } other { are # players }} online"
    command.details: [
      "Details:"
      " - Foo: <foo>"
      " - Bar: <bar>"
    ]
    command.splash_messages: [
      "Some funny message"
      "Another witty message"
      [ "A splash message", "with multiple lines" ]
    ]
    item: {
      m9: "Beretta M9 (price: <value>)"
      m4a1: "Colt M4A1 (price: <value>)"
    }
     */

    @Test
    fun testGlossa() {
        val reload: Message = glossa.message("command.reload") {
            replace("plugin-name", Component.text("MyPlugin"))
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
            replace("foo", Component.text("foo"))
            replace("bar", Component.text("bar"))
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

    @Test
    fun otherTest() {
        val myPluginMessages: MyPluginMessages = glossa.createMessagesOrSomethingIdk(MyPluginMessages::class /* which defines the interface */)

        val reload: Message = myPluginMessages.command.reload()
        val players0 = myPluginMessages.command.players(players = 0)
        val players1 = myPluginMessages.command.players(players = 1)
        val players10 = myPluginMessages.command.players(players = 10)

        // `command.details` will have been defined as returning `Message`
        val details = myPluginMessages.command.details(foo = Component.text("foo"), bar = Component.text("bar"))

        // `command.splash_messages` will have been defined as returning `List<Message>`
        val splashMessages = myPluginMessages.command.splashMessages()

        // Entries under `item.*`:
        // - max depth of 1 (no subkeys are allowed under `m9` or `m4a1`)
        // - all accept the same parameters
        // maybe this can be relaxed? idk
        val itemM9 = myPluginMessages.item("m9", value = 100)
        val itemM4A1 = myPluginMessages.item("m4a1", value = 500)
    }
}
