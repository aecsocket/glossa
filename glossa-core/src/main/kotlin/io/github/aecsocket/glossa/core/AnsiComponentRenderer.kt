package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.flattener.ComponentFlattener
import net.kyori.adventure.text.flattener.FlattenerListener
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

// Adapted from https://github.com/KyoriPowered/ansi/blob/trunk/src/main/java/net/kyori/ansi/ColorLevel.java

fun interface ColorLevel {
    fun escape(rgb: Int): String

    companion object {
        val TrueColor = ColorLevel { rgb ->
            "38;2;${rgb shr 16 and 0xff};${rgb shr 8 and 0xff};${rgb and 0xff}"
        }

        val Indexed16 = ColorLevel { rgb ->
            when (rgb) {
                0x000000 -> "30"
                0x0000aa -> "34"
                0x00aa00 -> "32"
                0x00aaaa -> "36"
                0xaa0000 -> "31"
                0xaa00aa -> "35"
                0xffaa00 -> "33"
                0xaaaaaa -> "37"
                0x555555 -> "90"
                0x5555ff -> "94"
                0x55ff55 -> "92"
                0x55ffff -> "96"
                0xff5555 -> "91"
                0xff55ff -> "95"
                0xffff55 -> "93"
                0xffffff -> "97"
                else -> "39" // reset
            }
        }

        fun colorLevel(): ColorLevel {
            System.getenv("COLORTERM")?.let {
                if (it == "truecolor" || it == "24bit")
                    return TrueColor
            }
            return Indexed16
        }
    }
}

class AnsiComponentRenderer(
    private val colorLevel: ColorLevel
) {
    fun render(input: Component): String {
        val result = StringBuilder()
        val styleStack = Stack<Style>()
        var colorDelta: Optional<TextColor>? = null
        val decorationDelta = HashMap<TextDecoration, Boolean>()

        fun currentStyle() = Style.style().apply { builder ->
            styleStack.forEach { builder.merge(it) }
        }.build()

        fun appendDeltas() {
            val escapeCodes = ArrayList<String>()
            var hasReset = false
            colorDelta?.let { color ->
                hasReset = color.isEmpty
                escapeCodes += if (hasReset) "0" else colorLevel.escape(color.get().value())
            }
            decorationDelta.forEach { (decoration, enabled) ->
                // if we're disabling something, no need to add another escape code; we've already reset
                if (!enabled && hasReset) return@forEach
                escapeCodes += when (decoration) {
                    TextDecoration.BOLD -> if (enabled) "1" else "22"
                    TextDecoration.ITALIC -> if (enabled) "3" else "23"
                    TextDecoration.UNDERLINED -> if (enabled) "4" else "24"
                    TextDecoration.OBFUSCATED -> if (enabled) "5" else "25"
                    TextDecoration.STRIKETHROUGH -> if (enabled) "9" else "29"
                }
            }

            if (escapeCodes.isNotEmpty()) {
                result.append("\u001b[")
                    .append(escapeCodes.joinToString(";"))
                    .append("m")
            }
            colorDelta = null
            decorationDelta.clear()
        }

        ComponentFlattener.basic().flatten(input, object : FlattenerListener {
            override fun pushStyle(style: Style) {
                styleStack.push(style)

                style.color()?.let {
                    colorDelta = Optional.of(it)
                }
                style.decorations().forEach { (decoration, state) ->
                    when (state) {
                        TextDecoration.State.TRUE -> decorationDelta[decoration] = true
                        TextDecoration.State.FALSE -> decorationDelta[decoration] = false
                        else -> {}
                    }
                }
            }

            override fun popStyle(style: Style) {
                styleStack.pop()
                val preStyle = currentStyle()

                style.color()?.let {
                    colorDelta = Optional.ofNullable(preStyle.color())
                }
                style.decorations().forEach { (decoration, state) ->
                    when (state) {
                        TextDecoration.State.TRUE, TextDecoration.State.FALSE -> {
                            decorationDelta[decoration] = preStyle.hasDecoration(decoration)
                        }
                        else -> {}
                    }
                }
            }

            override fun component(text: String) {
                appendDeltas()
                result.append(text)
            }
        })

        // final reset
        appendDeltas()

        return result.toString()
    }
}

val DefaultAnsiComponentRenderer = AnsiComponentRenderer(ColorLevel.colorLevel())
