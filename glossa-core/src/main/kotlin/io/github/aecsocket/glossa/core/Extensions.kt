package io.github.aecsocket.glossa.core

import com.ibm.icu.text.MessageFormat
import java.text.FieldPosition

fun MessageFormat.format(args: Map<String, Any>): String {
    val result = StringBuffer()
    format(args, result, FieldPosition(0))
    return result.toString()
}
