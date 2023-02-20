package io.github.aecsocket.glossa.core

import net.kyori.adventure.text.Component
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

@Target(AnnotationTarget.FUNCTION)
annotation class MessageKey(
    val value: String = ""
)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Placeholder(
    val value: String = ""
)

@Target(AnnotationTarget.PROPERTY)
annotation class SectionKey(
    val value: String = ""
)

interface MessageProxy<T> {
    val default: T

    fun locale(locale: Locale): T
}

private data class MessageProxyModel(
    val type: Class<*>,
    val getCallback: (method: Method) -> MethodCallback
)

private sealed interface MethodCallback {
    fun interface CreateMessage : MethodCallback {
        fun create(locale: Locale, args: Array<Any>): Any
    }

    fun interface GetSection : MethodCallback {
        fun section(): MessageProxyModel
    }
}

private fun interface MessageParameter {
    fun model(model: MessageArgs.Model, arg: Any)
}

private fun interface MessageProvider {
    fun get(locale: Locale, args: MessageArgs): Any
}

private val namingScheme = NamingScheme.SnakeCase

private fun Glossa.messageProxyModel(type: KClass<*>, baseMessageKey: String): MessageProxyModel {
    if (!type.isAbstract)
        throw IllegalArgumentException("Type must be abstract")

    val model = HashMap<Method, MethodCallback>()
    type.declaredMembers.forEach { member ->
        fun error(message: String): Nothing =
            throw IllegalArgumentException("${member.name}: $message")

        when (member) {
            is KFunction -> {
                // message key
                val messageKey = baseMessageKey + (member.findAnnotation<MessageKey>()?.value
                    ?.ifEmpty { namingScheme.coerceName(member.name) }
                    ?: error("Must be annotated with ${MessageKey::class.simpleName}"))

                val messageParams = (1 until member.parameters.size).map { paramIdx ->
                    val param = member.parameters[paramIdx]
                    fun error(message: String): Nothing =
                        throw IllegalArgumentException("${member.name} param ${paramIdx+1} (${param.name}): $message")

                    val placeholder = param.findAnnotation<Placeholder>()?.value?.ifEmpty { null }
                        ?: namingScheme.coerceName(param.name
                            ?: error("Must have parameter name or be annotated with ${Placeholder::class.simpleName}"))

                    when (param.type.classifier) {
                        Component::class -> MessageParameter { model, arg ->
                            model.replace(placeholder, arg as Component)
                        }
                        else -> MessageParameter { model, arg ->
                            model.format(placeholder, arg)
                        }
                    }
                }

                val returnType = member.returnType
                val messageProvider = when {
                    returnType.classifier == List::class
                    && returnType.arguments[0].type?.classifier == Component::class ->
                        MessageProvider { locale, args ->
                            message(locale, messageKey, args)
                        }
                    returnType.classifier == List::class
                    && returnType.arguments[0].type?.classifier == List::class
                    && returnType.arguments[0].type?.arguments?.get(0)?.type?.classifier == Component::class ->
                        MessageProvider { locale, args ->
                            messageList(locale, messageKey, args)
                        }
                    else -> error("Must return Message (= List<Component>) or List<Message>")
                }

                val method = member.javaMethod
                    ?: error("No corresponding Java method")
                model[method] = MethodCallback.CreateMessage { locale, args ->
                    messageProvider.get(locale, messageArgs {
                        messageParams.forEachIndexed { idx, param ->
                            param.model(this, args[idx])
                        }
                    })
                }
            }
            is KProperty -> {
                // subsection
                val sectionKey = member.findAnnotation<SectionKey>()?.value
                    ?.ifEmpty { namingScheme.coerceName(member.name) }
                    ?: error("Must be annotated with ${SectionKey::class.simpleName}")

                val sectionType = member.returnType.classifier as? KClass<*>
                    ?: error("Must return class")
                val sectionProxyModel = messageProxyModel(
                    sectionType,
                    "$baseMessageKey$sectionKey."
                )

                val getter = member.javaGetter
                    ?: error("No corresponding Java getter method")
                model[getter] = MethodCallback.GetSection { sectionProxyModel }
            }
        }
    }

    return MessageProxyModel(
        type.java
    ) { method ->
        model[method] ?: throw IllegalStateException("No proxy model method for $method")
    }
}

private fun MessageProxyModel.createProxy(locale: Locale): Any {
    val sectionProxies = HashMap<Method, Any>()
    return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(type)) { _, method, args ->
        when (val callback = getCallback(method)) {
            is MethodCallback.CreateMessage -> callback.create(locale, args ?: emptyArray())
            is MethodCallback.GetSection -> sectionProxies.computeIfAbsent(method) {
                callback.section().createProxy(locale)
            }
        }
    }
}

fun <T : Any> Glossa.messageProxy(type: KClass<T>): MessageProxy<T> {
    val proxyModel = messageProxyModel(type, "")
    return object : MessageProxy<T> {
        val proxies = HashMap<Locale, T>()
        override val default = locale(locale)

        override fun locale(locale: Locale): T {
            return proxies.computeIfAbsent(locale) {
                @Suppress("UNCHECKED_CAST")
                proxyModel.createProxy(locale) as T
            }
        }
    }
}

inline fun <reified T : Any> Glossa.messageProxy() =
    messageProxy(T::class)

/*
fun <T : Any> Glossa.messageProxy(type: Class<T>): MessageProxy<T> {
    if (!type.isInterface)
        throw IllegalArgumentException("Type must be interface")

    val model = HashMap<Method, MethodCallback>()
    type.declaredMethods.forEach { method ->
        fun error(message: String): Nothing =
            throw IllegalArgumentException("${method.name}: $message")

        val messageKey = method.getAnnotation(MessageKey::class.java)?.value
            ?.ifEmpty { namingScheme.coerceName(method.name) }
            ?: error("Must be annotated with ${MessageKey::class.java}")

        val methodParams = method.parameters.toMutableList()
        val messageParams: List<MessageParameter> = methodParams.mapIndexed { idx, methodParam ->
            val placeholder = methodParam.getAnnotation(Placeholder::class.java)?.value
                ?.ifEmpty { namingScheme.coerceName(methodParam.name) }
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
*/
