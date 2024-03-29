package io.github.aecsocket.glossa

import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod
import net.kyori.adventure.text.Component

/**
 * Determines a method which maps to a message key under the current section.
 *
 * If blank or not specified, the method name is coerced using [coerceName].
 */
@Target(AnnotationTarget.FUNCTION) annotation class MessageKey(val value: String = "")

/**
 * Determines a method parameter which maps to a placeholder under the method's message key.
 *
 * If blank or not specified, the parameter name is coerced using [coerceName].
 */
@Target(AnnotationTarget.VALUE_PARAMETER) annotation class Placeholder(val value: String = "")

/**
 * Determines a property getter which maps to a section key under the current section.
 *
 * If blank or not specified, the property name is coerced using [coerceName].
 */
@Target(AnnotationTarget.PROPERTY) annotation class SectionKey(val value: String = "")

/** Provides access to a generic message proxy created by [messageProxy]. */
interface MessageProxy<T : Any> {
  /** Gets the message proxy for the default locale. */
  val default: T

  /** Gets the message proxy for a specific locale. */
  fun forLocale(locale: Locale): T
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

private fun Glossa.messageProxyModel(type: KClass<*>, baseMessageKey: String): MessageProxyModel {
  if (!type.isAbstract) throw IllegalArgumentException("Type must be abstract")

  val model = ConcurrentHashMap<Method, MethodCallback>()
  type.declaredMembers.forEach { member ->
    fun error(message: String): Nothing = throw IllegalArgumentException("${member.name}: $message")

    when (member) {
      is KFunction -> {
        // message key
        val thisMessageKey =
            member.findAnnotation<MessageKey>()?.value?.ifEmpty { null } ?: coerceName(member.name)
        val messageKey = baseMessageKey + thisMessageKey

        val messageParams =
            (1 until member.parameters.size).map { paramIdx ->
              val param = member.parameters[paramIdx]
              fun error(message: String): Nothing =
                  throw IllegalArgumentException(
                      "${member.name} param ${paramIdx+1} (${param.name}): $message")

              val placeholder =
                  param.findAnnotation<Placeholder>()?.value?.ifEmpty { null }
                      ?: coerceName(
                          param.name
                              ?: error(
                                  "Must have parameter name or be annotated with ${Placeholder::class.simpleName}"))

              when (param.type.classifier) {
                Component::class ->
                    MessageParameter { model, arg -> model.replace(placeholder, arg as Component) }
                else -> MessageParameter { model, arg -> model.format(placeholder, arg) }
              }
            }

        val returnType = member.returnType
        val messageProvider =
            when {
              returnType.classifier == List::class &&
                  returnType.arguments[0].type?.classifier == Component::class ->
                  MessageProvider { locale, args -> message(messageKey, locale, args) }
              returnType.classifier == List::class &&
                  returnType.arguments[0].type?.classifier == List::class &&
                  returnType.arguments[0].type?.arguments?.get(0)?.type?.classifier ==
                      Component::class ->
                  MessageProvider { locale, args -> messageList(messageKey, locale, args) }
              else -> error("Must return Message (= List<Component>) or List<Message>")
            }

        val method = member.javaMethod ?: error("No corresponding Java method")
        model[method] =
            MethodCallback.CreateMessage { locale, args ->
              messageProvider.get(
                  locale,
                  messageArgs {
                    messageParams.forEachIndexed { idx, param -> param.model(this, args[idx]) }
                  })
            }
      }
      is KProperty -> {
        // subsection
        val sectionKey =
            member.findAnnotation<SectionKey>()?.value?.ifEmpty { null } ?: coerceName(member.name)

        val sectionType = member.returnType.classifier as? KClass<*> ?: error("Must return class")
        val sectionProxyModel = messageProxyModel(sectionType, "$baseMessageKey$sectionKey.")

        val getter = member.javaGetter ?: error("No corresponding Java getter method")
        model[getter] = MethodCallback.GetSection { sectionProxyModel }
      }
    }
  }

  return MessageProxyModel(type.java) { method ->
    model[method] ?: throw IllegalStateException("No proxy model method for $method")
  }
}

private fun MessageProxyModel.createProxy(locale: Locale): Any {
  val sectionProxies = ConcurrentHashMap<Method, Any>()
  return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(type)) { _, method, args ->
    when (val callback = getCallback(method)) {
      is MethodCallback.CreateMessage -> callback.create(locale, args ?: emptyArray())
      is MethodCallback.GetSection ->
          sectionProxies.computeIfAbsent(method) { callback.section().createProxy(locale) }
    }
  }
}

/**
 * Generates a [MessageProxy] which uses the underlying [T] type as a type-safe API for message
 * generation.
 *
 * The type provided must:
 * - be an interface
 * - only hold methods which map to message keys
 * - only hold properties which map to subsection keys
 */
fun <T : Any> Glossa.messageProxy(type: KClass<T>): MessageProxy<T> {
  val proxyModel = messageProxyModel(type, "")
  return object : MessageProxy<T> {
    val proxies = ConcurrentHashMap<Locale, T>()
    override val default = forLocale(locale)

    override fun forLocale(locale: Locale): T {
      return proxies.computeIfAbsent(locale) {
        @Suppress("UNCHECKED_CAST")
        proxyModel.createProxy(locale) as T
      }
    }
  }
}

inline fun <reified T : Any> Glossa.messageProxy() = messageProxy(T::class)
