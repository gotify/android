package com.github.gotify.messages

import com.github.gotify.client.model.Message

internal object Extras {
    fun useMarkdown(message: Message): Boolean = useMarkdown(message.extras)

    fun useMarkdown(extras: Map<String, Any>?): Boolean {
        if (extras == null) {
            return false
        }

        val display: Any? = extras["client::display"]
        if (display !is Map<*, *>) {
            return false
        }

        return "text/markdown" == display["contentType"]
    }

    fun <T> getNestedValue(clazz: Class<T>, extras: Map<String, Any>?, vararg keys: String): T? {
        var value: Any? = extras

        keys.forEach { key ->
            if (value == null) {
                return null
            }

            value = (value as Map<*, *>)[key]
        }

        if (!clazz.isInstance(value)) {
            return null
        }

        return clazz.cast(value)
    }
}
