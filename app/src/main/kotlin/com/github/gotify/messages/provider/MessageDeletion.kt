package com.github.gotify.messages.provider

import com.github.gotify.client.model.Message

internal class MessageDeletion(
    val message: Message,
    val allPosition: Int,
    val appPosition: Int
)
