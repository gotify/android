package com.github.gotify.messages

import android.app.Activity
import androidx.lifecycle.ViewModel
import coil.target.Target
import com.github.gotify.Settings
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.MessageApi
import com.github.gotify.messages.provider.ApplicationHolder
import com.github.gotify.messages.provider.MessageFacade
import com.github.gotify.messages.provider.MessageState

internal class MessagesModel(parentView: Activity) : ViewModel() {
    val settings = Settings(parentView)
    val client = ClientFactory.clientToken(settings)
    val appsHolder = ApplicationHolder(parentView, client)
    val messages = MessageFacade(client.createService(MessageApi::class.java), appsHolder)

    // we need to keep the target references otherwise they get gc'ed before they can be called.
    val targetReferences = mutableListOf<Target>()

    var appId = MessageState.ALL_MESSAGES
}
