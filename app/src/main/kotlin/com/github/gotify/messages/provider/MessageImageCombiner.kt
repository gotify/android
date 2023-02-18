package com.github.gotify.messages.provider

import com.github.gotify.client.model.Application
import com.github.gotify.client.model.Message

internal object MessageImageCombiner {
    fun combine(messages: List<Message>, applications: List<Application>): List<MessageWithImage> {
        val appIdToImage = appIdToImage(applications)
        return messages.map { MessageWithImage(message = it, image = appIdToImage[it.appid]) }
    }

    private fun appIdToImage(applications: List<Application>): Map<Long, String> {
        val map = mutableMapOf<Long, String>()
        applications.forEach {
            map[it.id] = it.image
        }
        return map
    }
}
