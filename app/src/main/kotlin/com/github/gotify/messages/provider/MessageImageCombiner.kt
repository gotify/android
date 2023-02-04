package com.github.gotify.messages.provider

import com.github.gotify.client.model.Application
import com.github.gotify.client.model.Message

internal object MessageImageCombiner {
    fun combine(messages: List<Message>, applications: List<Application>): List<MessageWithImage> {
        val appIdToImage = appIdToImage(applications)
        val result = mutableListOf<MessageWithImage>()
        messages.forEach {
            val messageWithImage = MessageWithImage()
            messageWithImage.message = it
            messageWithImage.image = appIdToImage[it.appid]
            result.add(messageWithImage)
        }
        return result
    }

    fun appIdToImage(applications: List<Application>): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        applications.forEach {
            map[it.id] = it.image
        }
        return map
    }
}
