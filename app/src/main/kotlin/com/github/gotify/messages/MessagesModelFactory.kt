package com.github.gotify.messages

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class MessagesModelFactory(
    var modelParameterActivity: Activity
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == MessagesModel::class.java) {
            @Suppress("UNCHECKED_CAST")
            return modelClass.cast(MessagesModel(modelParameterActivity)) as T
        }
        throw IllegalArgumentException(
            "modelClass parameter must be of type ${MessagesModel::class.java.name}"
        )
    }
}
