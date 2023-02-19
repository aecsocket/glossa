package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals

const val E_GREEN = "92"
const val E_BLUE = "94"
const val E_BOLD = "1"
const val E_NO_BOLD = "22"
const val E_ITALIC = "3"
const val E_NO_ITALIC = "23"
const val RESET = "\u001b[0m"

class AnsiComponentRendererTest {
    fun ansi(input: Component) = AnsiComponentRenderer(ColorLevel.Indexed16).render(input)

    fun esc(vararg codes: String) = "\u001b[${codes.joinToString(";")}m"

    @Test
    fun testText() {
        assertEquals("Hello world!", ansi(
            text("Hello world!")
        ))
        assertEquals("Components: OneTwo", ansi(
            text("Components: ")
                .append(text("One"))
                .append(text("Two"))
        ))
    }

    @Test
    fun testColor() {
        assertEquals("${esc(E_GREEN)}Green$RESET", ansi(
            text("Green", GREEN)
        ))
        assertEquals("${esc(E_BLUE)}Blue$RESET", ansi(
            text("Blue", BLUE)
        ))
        assertEquals("${esc(E_GREEN)}Green${esc(E_BLUE)}Blue$RESET", ansi(
            text("Green", GREEN)
                .append(text("Blue", BLUE))
        ))
        assertEquals("${esc(E_GREEN)}Green${esc(E_BLUE)}Blue${esc(E_GREEN)}Default$RESET", ansi(
            text("Green", GREEN)
                .append(text("Blue", BLUE))
                .append(text("Default"))
        ))
    }

    @Test
    fun testDecorations() {
        assertEquals("${esc(E_BOLD)}Bold${esc(E_NO_BOLD)}", ansi(
            text("Bold", null, TextDecoration.BOLD)
        ))
        assertEquals("${esc(E_ITALIC)}Italic${esc(E_NO_ITALIC)}", ansi(
            text("Italic", null, TextDecoration.ITALIC)
        ))
        assertEquals("${esc(E_BOLD)}Bold${esc(E_ITALIC)}AndItalic${esc(E_NO_ITALIC)}JustBold${esc(E_NO_BOLD)}", ansi(
            text("Bold", null, TextDecoration.BOLD)
                .append(text("AndItalic", null, TextDecoration.ITALIC))
                .append(text("JustBold"))
        ))
    }

    @Test
    fun testColorAndDecorations() {
        assertEquals("${esc(E_GREEN)}Green${esc(E_BOLD)}AndBold${esc(E_BLUE, E_NO_BOLD)}JustBlue${esc(E_BOLD)}AndBold$RESET", ansi(
            text("Green", GREEN)
                .append(text("AndBold", null, TextDecoration.BOLD))
                .append(text("JustBlue", BLUE)
                    .append(text("AndBold", null, TextDecoration.BOLD))
                )
        ))
    }
}
