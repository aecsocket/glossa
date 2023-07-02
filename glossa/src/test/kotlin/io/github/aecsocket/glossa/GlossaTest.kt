package io.github.aecsocket.glossa

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val gson = GsonComponentSerializer.gson()

fun assertMessage(expected: Message, actual: Message) {
    assertEquals(
        expected.map { gson.serialize(it) },
        actual.map { gson.serialize(it) }
    )
}

fun assertMessageList(expected: List<Message>, actual: List<Message>) {
    assertEquals(
        expected.map { x -> x.map { gson.serialize(it) } },
        actual.map { x -> x.map { gson.serialize(it )} },
    )
}

class GlossaTest {
    @Test
    fun testBuilder() {
        val glossa = glossaStandard(
            defaultLocale = Locale.ENGLISH,
            invalidMessageProvider = InvalidMessageProvider.Default,
        ) {
            substitutions {
                substitution("a", text("A"))
            }

            styles {
                style("a", Style.empty())
            }

            translation(Locale.ENGLISH) {
                message("a", "A")
                message("b", "B")
                message("c", "C")
            }

            translation(Locale.FRENCH) {
                message("a", "A")
            }
        }

        assertEquals(1, glossa.countSubstitutions())
        assertEquals(1, glossa.countStyles())
        assertEquals(2, glossa.countLocales())
        assertEquals(3, glossa.countMessages())
    }

    val english: Locale = Locale.ENGLISH
    val french: Locale = Locale.FRENCH

    val glossa = glossaStandard(
        defaultLocale = english,
        invalidMessageProvider = InvalidMessageProvider.Default,
    ) {
        translation(english) {
            message("hello_world", "Hello World!")

            message("a_message", "Simple message")

            message("multiline", """
                Line one
                Line two
            """.trimIndent())

            messageList("a_message_list",
                "Message one",
                "Message two",
            )

            messageList("multiline_list",
                "Message one with 1 line",
                """
                    Message two
                    with 2 lines
                """.trimIndent(),
            )

            section("section") {
                message("child", "Child message")
            }

            message("with_replacement", "a = <a>")

            message("with_format_number", "num = {num, number}")

            message("with_format_date", "dt = {dt, date, short}")

            message("with_format_plural", """
                Added {num_items, plural,
                  =0 {no items}
                  one {# item}
                  other {# items}
                }.
            """.trimIndent())
        }

        translation(french) {
            message("hello_world", "FR Hello World!")

            message("multiline", """
                FR Line one
                FR Line two
            """.trimIndent())
        }
    }

    @Test
    fun basic() {
        assertMessage(
            listOf(text("Hello World!"),),
            glossa.message("hello_world", english)
        )

        assertMessageList(
            listOf(
                listOf(text("Message one")),
                listOf(text("Message two")),
            ),
            glossa.messageList("a_message_list", french)
        )

        assertMessage(
            listOf(text("Child message")),
            glossa.message("section.child", english)
        )
    }

    @Test
    fun multiline() {
        assertMessage(
            listOf(
                text("Line one"),
                text("Line two"),
            ),
            glossa.message("multiline", english)
        )

        assertMessage(
            listOf(
                text("FR Line one"),
                text("FR Line two"),
            ),
            glossa.message("multiline", french)
        )

        assertMessageList(
            listOf(
                listOf(text("Message one with 1 line")),
                listOf(
                    text("Message two"),
                    text("with 2 lines"),
                ),
            ),
            glossa.messageList("multiline_list", english)
        )
    }

    @Test
    fun locales() {
        assertMessage(
            listOf(text("FR Hello World!")),
            glossa.message("hello_world", french)
        )

        assertMessage(
            listOf(text("Hello World!")),
            glossa.message("hello_world", english)
        )
    }

    @Test
    fun fallbacks() {
        assertMessage(
            listOf(text("Simple message")),
            glossa.message("a_message", english)
        )

        assertMessage(
            listOf(text("Simple message")),
            glossa.message("a_message", french)
        )

        assertMessage(
            listOf(text("missing_key")),
            glossa.message("missing_key", english)
        )
    }

    @Test
    fun invalidMessageProvider() {
        val isMissing = AtomicBoolean()
        val isInvalidType = AtomicBoolean()

        fun reset() {
            isMissing.set(false)
            isInvalidType.set(false)
        }

        val glossa = glossaStandard(
            defaultLocale = english,
            invalidMessageProvider = object : InvalidMessageProvider {
                override fun missing(key: String): Message {
                    isMissing.set(true)
                    return emptyList()
                }

                override fun invalidType(key: String, expected: MessageType): Message {
                    isInvalidType.set(true)
                    return emptyList()
                }
            }
        ) {
            translation(english) {
                message("a_message", "Message")

                messageList("a_message_list",
                    "Message one")
            }
        }

        glossa.message("a_message")
        assertFalse(isMissing.get())
        assertFalse(isInvalidType.get())

        glossa.message("missing_key")
        assertTrue(isMissing.get())
        assertFalse(isInvalidType.get())
        reset()

        glossa.messageList("a_message")
        assertFalse(isMissing.get())
        assertTrue(isInvalidType.get())
        reset()

        glossa.message("a_message_list")
        assertFalse(isMissing.get())
        assertTrue(isInvalidType.get())
        reset()
    }

    @Test
    fun replacements() {
        assertMessage(
            listOf(text("a = Hello")),
            glossa.message("with_replacement", english) {
                replace("a", text("Hello"))
            }
        )

        assertMessage(
            listOf(text("a = ").append(text("Red", NamedTextColor.RED))),
            glossa.message("with_replacement", english) {
                replace("a", text("Red", NamedTextColor.RED))
            }
        )
    }

    @Test
    fun formatNumber() {
        assertMessage(
            listOf(text("num = 123")),
            glossa.message("with_format_number", english) {
                format("num", 123)
            }
        )

        assertMessage(
            listOf(text("num = 1,234.5")),
            glossa.message("with_format_number", english) {
                format("num", 1234.5)
            }
        )

        assertMessage(
            // \u202f = narrow no-break space
            listOf(text("num = 1\u202f234,5")),
            glossa.message("with_format_number", french) {
                format("num", 1234.5)
            }
        )
    }

    @Test
    fun formatDate() {
        assertMessage(
            listOf(text("dt = 1/1/70")),
            glossa.message("with_format_date", english) {
                format("dt", Date(0))
            }
        )

        assertMessage(
            listOf(text("dt = 01/01/1970")),
            glossa.message("with_format_date", french) {
                format("dt", Date(0))
            }
        )
    }

    @Test
    fun formatPlural() {
        assertMessage(
            listOf(text("Added no items.")),
            glossa.message("with_format_plural", english) {
                format("num_items", 0)
            }
        )

        assertMessage(
            listOf(text("Added 1 item.")),
            glossa.message("with_format_plural", english) {
                format("num_items", 1)
            }
        )

        assertMessage(
            listOf(text("Added 2 items.")),
            glossa.message("with_format_plural", english) {
                format("num_items", 2)
            }
        )
    }

    interface Messages {
        fun helloWorld(): Message

        @MessageKey("a_message")
        fun withSpecialKey(): Message

        fun aMessageList(): List<Message>

        val section: Section
        interface Section {
            fun child(): Message
        }

        @SectionKey("section")
        val withSectionKey: Section

        fun withReplacement(
            a: Component,
        ): Message

        fun withFormatNumber(
            num: Float,
        ): Message

        fun withFormatDate(
            @Placeholder("dt") date: Date,
        ): Message
    }

    val messages = glossa.messageProxy<Messages>()
    val forEnglish = messages.forLocale(english)
    val forFrench = messages.forLocale(french)

    @Test
    fun messageProxyBasic() {
        assertMessage(
            listOf(text("Hello World!")),
            forEnglish.helloWorld()
        )

        assertMessage(
            listOf(text("FR Hello World!")),
            forFrench.helloWorld(),
        )

        assertMessageList(
            listOf(
                listOf(text("Message one")),
                listOf(text("Message two")),
            ),
            forEnglish.aMessageList()
        )

        assertMessage(
            listOf(text("Child message")),
            forEnglish.section.child()
        )
    }

    @Test
    fun messageProxyArguments() {
        assertMessage(
            listOf(text("a = Hello")),
            forEnglish.withReplacement(text("Hello"))
        )

        assertMessage(
            listOf(text("num = 1,234.5")),
            forEnglish.withFormatNumber(1234.5f)
        )
    }

    @Test
    fun messageProxyKeyOverrides() {
        assertMessage(
            listOf(text("Simple message")),
            forEnglish.withSpecialKey()
        )

        assertMessage(
            listOf(text("Child message")),
            forEnglish.withSectionKey.child()
        )

        assertMessage(
            listOf(text("dt = 1/1/70")),
            forEnglish.withFormatDate(Date(0))
        )
    }
}
