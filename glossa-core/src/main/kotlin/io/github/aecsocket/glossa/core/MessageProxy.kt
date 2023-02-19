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

interface MessageProxy<T> {
    val default: T

    fun locale(locale: Locale): T
}

private fun interface MethodCallback {
    fun run(locale: Locale, args: Array<Any>): Any
}

private fun interface MessageParameter {
    fun model(model: MessageArgs.Model, arg: Any)
}

private fun interface MessageProvider {
    fun get(locale: Locale, args: MessageArgs): Any
}

fun <T : Any> Glossa.messageProxy(type: Class<T>): MessageProxy<T> {
    if (!type.isInterface)
        throw IllegalArgumentException("Type must be interface")

    val model = HashMap<Method, MethodCallback>()
    type.declaredMethods.forEach { method ->
        fun error(message: String): Nothing =
            throw IllegalArgumentException("${method.name}: $message")

        val messageKey = method.getAnnotation(MessageKey::class.java)?.value
            ?: error("Must be annotated with ${MessageKey::class.java}")

        val methodParams = method.parameters.toMutableList()
        val messageParams: List<MessageParameter> = methodParams.mapIndexed { idx, methodParam ->
            val placeholder = methodParam.getAnnotation(Placeholder::class.java)?.value
                ?: error("Parameter ${idx + 1} must be annotated with ${Placeholder::class.simpleName}")

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

        model[method] = MethodCallback { locale, args ->
            messageProvider.get(locale, messageArgs {
                messageParams.forEachIndexed { idx, param ->
                    param.model(this, args[idx])
                }
            })
        }
    }

    val interfaces = arrayOf(type)
    return object : MessageProxy<T> {
        val proxies = HashMap<Locale, T>()
        override val default = locale(locale)

        override fun locale(locale: Locale): T {
            return proxies.computeIfAbsent(locale) {
                @Suppress("UNCHECKED_CAST")
                Proxy.newProxyInstance(javaClass.classLoader, interfaces) { _, method, args ->
                    model[method]!!.run(locale, args ?: emptyArray())
                } as T
            }
        }
    }
}

inline fun <reified T : Any> Glossa.messageProxy(): MessageProxy<T> =
    messageProxy(T::class.java)
