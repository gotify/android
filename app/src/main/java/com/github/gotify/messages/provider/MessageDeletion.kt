package com.github.gotify.messages.provider

import com.github.gotify.client.model.Message

class MessageDeletion(
    val message: Message,
    val allPosition: Int,
    val appPosition: Int
)
