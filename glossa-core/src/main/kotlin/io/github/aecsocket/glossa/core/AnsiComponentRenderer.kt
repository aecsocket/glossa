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

private const val START = "\u001b["
private const val END = "m"
private const val RESET = "0"

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
                result.append(START)
                    .append(escapeCodes.joinToString(";"))
                    .append(END)
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

//        val styleStack = Stack<Style>()
//        val nextCodes = ArrayList<String>()
//
//        fun currentStyle() = Style.style().apply { builder ->
//            styleStack.forEach { builder.merge(it) }
//        }.build()
//
//        fun appendCodes() {
//            if (nextCodes.isNotEmpty()) {
//                result.append(START)
//                    .append(nextCodes.joinToString(";"))
//                    .append(END)
//            }
//        }
//
//        fun Style.setDecorations() = decorations()
//            .filter { (_, state) -> state == TextDecoration.State.TRUE }
//            .map { (decoration) -> decoration }
//
//        fun TextDecoration.escape() = when (this) {
//            TextDecoration.BOLD -> "1"
//            TextDecoration.ITALIC -> "3"
//            TextDecoration.UNDERLINED -> "4"
//            TextDecoration.OBFUSCATED -> "5"
//            TextDecoration.STRIKETHROUGH -> "9"
//        }
//
//        ComponentFlattener.basic().flatten(input, object : FlattenerListener {
//            override fun pushStyle(style: Style) {
//                val oldStyle = currentStyle()
//                styleStack.push(style)
//                var color: TextColor? = null
//                val decorations = HashSet<TextDecoration>()
//
//                // the style we just entered...
//                style.color()?.let {
//                    if (it != oldStyle.color()) {
//                        // ...changed the color
//                        color = it
//                    }
//                }
//                var changedDecorations = false
//                style.decorations().forEach { (decoration, state) ->
//                    when (state) {
//                        TextDecoration.State.TRUE -> {
//                            // ...added a decoration, so we add it to the escapes
//                            decorations += decoration
//                        }
//                        TextDecoration.State.FALSE -> {
//                            // ...disabled a decoration, so we must rebuild the current escape from scratch
//                            changedDecorations = true
//                        }
//                        else -> {}
//                    }
//                }
//
//                nextCodes.clear()
//                if (changedDecorations) {
//
//
//                    // rebuilding the escape from scratch
//                    // merge all styles up to this point
//                    val currentStyle = Style.style().apply { builder ->
//                        styleStack.forEach { builder.merge(it) }
//                    }.build()
//
//                    // rebuild the escape
//                    color = currentStyle.color()
//                    currentStyle.decorations().forEach { (decoration, state) ->
//                        if (state == TextDecoration.State.TRUE) {
//                            decorations += decoration
//                        }
//                    }
//                }
//
//                color?.let { nextCodes += colorLevel.escape(it.value()) }
//                decorations.forEach { decoration ->
//                    nextCodes += when (decoration) {
//                        TextDecoration.BOLD -> "1"
//                        TextDecoration.ITALIC -> "3"
//                        TextDecoration.UNDERLINED -> "4"
//                        TextDecoration.OBFUSCATED -> "5"
//                        TextDecoration.STRIKETHROUGH -> "9"
//                    }
//                }
//            }
//
//            override fun popStyle(style: Style) {
//                styleStack.pop()
//                val currentStyle by lazy { currentStyle() }
//                var color: TextColor? = null
//                val decorations = HashSet<TextDecoration>()
//
//                // the style we just exited...
//                style.color()?.let {
//                    // ...changed the color
//                    color = currentStyle.color()
//                }
//                var changedDecorations  = false
//                style.decorations().forEach { (_, state) ->
//                    when (state) {
//                        TextDecoration.State.TRUE, TextDecoration.State.FALSE -> {
//                            // ...changed at least one decoration, so we do a full decoration rebuild
//                            changedDecorations = true
//                        }
//                        else -> {}
//                    }
//                }
//
//                // ...so we build the escape sequence
//                nextCodes.clear()
//                if (changedDecorations) {
//                    // a decoration was changed; do a full rebuild
//                    nextCodes += RESET
//                    decorations += currentStyle.setDecorations()
//                }
//                color?.let { nextCodes += colorLevel.escape(it.value()) }
//                decorations.forEach { decoration ->
//                    nextCodes += decoration.escape()
//                }
//            }
//
//            override fun component(text: String) {
//                appendCodes()
//                result.append(text)
//            }
//        })
//
//        return result.toString()
//
//        ComponentFlattener.basic().flatten(input, object : FlattenerListener {
//            val styleStack = Stack<Style>()
//
//            override fun pushStyle(style: Style) {
//                styleStack.push(style)
//            }
//
//            override fun component(text: String) {
//                val styleBuilder = Style.style()
//                styleStack.forEach { styleBuilder.merge(it) }
//                val currentStyle = styleBuilder.build()
//
//                val ansiCodes = ArrayList<String>()
//                currentStyle.color()?.let { color ->
//                    ansiCodes += colorLevel.escape(color.value())
//                }
//                currentStyle.decorations().forEach { (decoration, state) ->
//                    if (state == TextDecoration.State.TRUE) {
//                        ansiCodes += when (decoration) {
//                            TextDecoration.BOLD -> "1"
//                            TextDecoration.ITALIC -> "3"
//                            TextDecoration.UNDERLINED -> "4"
//                            TextDecoration.OBFUSCATED -> "5"
//                            TextDecoration.STRIKETHROUGH -> "9"
//                            else -> throw IllegalStateException("Invalid decoration $decoration")
//                        }
//                    }
//                }
//
//                if (ansiCodes.isNotEmpty()) {
//                    ansiCodes.add(0, RESET)
//                    result.append(START)
//                        .append(ansiCodes.joinToString(";"))
//                        .append(END)
//                }
//                result.append(text)
//            }
//
//            override fun popStyle(style: Style) {
//                styleStack.pop()
//            }
//        })
//        return "$result$START$RESET$END"
    }
}

val DefaultAnsiComponentRenderer = AnsiComponentRenderer(ColorLevel.colorLevel())
