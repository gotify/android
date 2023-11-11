package com.github.gotify.messages.provider

import com.github.gotify.client.model.Message

@Suppress("EqualsOrHashCode")
internal data class MessageWithImage(
    val message: Message,
    val image: String?
) {
    override fun equals(other: Any?): Boolean {
        if (other !is MessageWithImage) return false
        return image == other.image && message == other.message
    }
}
