package com.github.gotify.messages.provider

import com.github.gotify.client.model.Message

internal class MessageWithImage {
    lateinit var message: Message
    var image: String? = null
}
