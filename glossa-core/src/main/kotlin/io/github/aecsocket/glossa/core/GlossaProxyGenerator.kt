package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Locale

@Target(AnnotationTarget.FUNCTION)
annotation class MessageKey(
    val value: String
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Placeholder(
    val value: String
)

private fun interface MethodCallback {
    fun run(args: Array<Any>): Any
}

private fun interface MessageParameter {
    fun model(model: MessageArgs.Model, arg: Any)
}

private fun interface MessageProvider {
    fun get(locale: Locale, args: MessageArgs): Any
}

fun <T : Any> Glossa.createMessages(type: Class<T>): T {
    if (!type.isInterface)
        throw IllegalArgumentException("Type must be interface")

    val model = HashMap<Method, MethodCallback>()
    type.declaredMethods.forEach { method ->
        fun error(message: String): Nothing =
            throw IllegalArgumentException("${method.name}: $message")

        val messageKey = method.getAnnotation(MessageKey::class.java)?.value
            ?: error("Must be annotated with ${MessageKey::class.java}")

        val methodParams = method.parameters
        if (methodParams.isEmpty() || methodParams[0].type != Locale::class.java)
            error("Parameters must be (Locale, ...)")

        val messageParams: List<MessageParameter> = (1 until methodParams.size).map { i ->
            val methodParam = methodParams[i]
            val placeholder = methodParam.getAnnotation(Placeholder::class.java)?.value
                ?: error("Parameter ${i+1} must be annotated with ${Placeholder::class.simpleName}")

            when (methodParam.type) {
                Component::class.java -> MessageParameter { model, arg ->
                    model.replace(placeholder, arg as Component)
                }
                else -> MessageParameter { model, arg ->
                    model.format(placeholder, arg)
                }
            }
        }

        val messageProvider: MessageProvider = when (method.genericReturnType.toString()) {
            "java.util.List<net.kyori.adventure.text.Component>" -> MessageProvider { locale, args ->
                message(locale, messageKey, args)
            }
            "java.util.List<java.util.List<net.kyori.adventure.text.Component>>" -> MessageProvider { locale, args ->
                messageList(locale, messageKey, args)
            }
            else -> error("Must return Message (= List<Component>) or List<Message>")
        }

        model[method] = MethodCallback { args ->
            val locale = args[0] as Locale
            messageProvider.get(locale, messageArgs {
                messageParams.forEachIndexed { idx, param ->
                    param.model(this, args[idx + 1])
                }
            })
        }
    }

    val proxy = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(type)) { _, method, args ->
        model[method]!!.run(args)
    }
    @Suppress("UNCHECKED_CAST")
    return proxy as T
}

inline fun <reified T : Any> Glossa.createMessages() = createMessages(T::class.java)
