package com.github.gotify.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.gotify.ApplicationState
import com.github.gotify.Repository
import com.github.gotify.client.model.Application
import com.github.gotify.client.model.Message
import com.github.gotify.messages.provider.MessageWithImage
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the messages screen.
 *
 * @param repository The repository to get the data from and to send the data to.
 */
internal class MessagesViewModel(private val repository: Repository) : ViewModel() {
    private val loadingMore = AtomicBoolean(false)
    private val shadowDeletedMessages: MutableStateFlow<Set<Long>> = MutableStateFlow(emptySet())
    private val currentApp = MutableStateFlow<Application?>(null)
    private val _refreshing = MutableStateFlow(false)

    /**
     * Whether the messages are currently being refreshed.
     */
    val refreshing = _refreshing

    /**
     * The state of the applications.
     */
    val applicationsState: StateFlow<List<ApplicationState>> = repository.applicationsState

    /**
     * The current menu mode.
     */
    val menuMode: StateFlow<MenuMode> = currentApp.map { app ->
        app?.let { AppMenuMode(it) } ?: AllAppMenuMode
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = AllAppMenuMode
    )

    /**
     * An observable list of messages. The messages are filtered by the current application.
     */
    private val messages: StateFlow<List<Message>> = currentApp.flatMapLatest { app ->
        when (app) {
            null -> repository.allMessages
            else -> repository.getMessages(app)
        }
    }.distinctUntilChanged().onEach { loadingMore.set(false) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    /**
     * An observable list of messages with images.
     * The messages are filtered by the current application and any shadow deleted messages.
     * The messages are sorted by id in descending order.
     */
    internal val messagesWithImage: StateFlow<List<MessageWithImage>> = combine(
        messages,
        applicationsState,
        shadowDeletedMessages
    ) { messages, apps, shadowDeletedMessages ->
        val appsImages =
            apps.associateBy { it.application.id }.mapValues { it.value.application.image }
        messages.filterNot { shadowDeletedMessages.contains(it.id) }.sortedByDescending { it.id }
            .map { message ->
                MessageWithImage(message, appsImages[message.appid])
            }
    }.distinctUntilChanged().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    /**
     * Refreshes all messages and applications.
     */
    fun onRefreshAll() {
        viewModelScope.launch {
            repository.fetchApps()
            repository.refreshAllMessages()
        }
    }

    /**
     * Refreshes the list of messages.
     */
    fun onRefreshMessages() {
        viewModelScope.launch {
            _refreshing.value = true
            currentApp.value?.let { repository.refreshMessages(it) }
                ?: repository.refreshAllMessages()
            _refreshing.value = false
        }
    }

    /**
     * Selects an application. The messages will be filtered by the selected application.
     */
    fun onSelectApplication(app: Application) {
        currentApp.value = app
    }

    /**
     * Deselects the application. The messages will not be filtered by any application.
     */
    fun onDeselectApplication() {
        currentApp.value = null
    }

    /**
     * Shadows a message. The message will not be displayed in the list of messages.
     */
    fun onShadowDeleteMessage(message: Message) {
        shadowDeletedMessages.value += message.id
    }

    /**
     * Unshadows a message. The message will be displayed in the list of messages.
     */
    fun onUnShadowDeleteMessage(message: Message) {
        shadowDeletedMessages.value -= message.id
    }

    /**
     * Deletes a message. The message will be removed from the list of messages and the server.
     * If the message could not be deleted from the server, the message will be unshadowed.
     */
    fun onDeleteMessage(message: Message) {
        viewModelScope.launch {
            if (!repository.deleteMessage(message)) {
                onUnShadowDeleteMessage(message)
            }
        }
    }

    /**
     * Deletes all messages. The messages will be removed from the list of messages and the server.
     *
     * @param menuMode The menu mode to delete the messages from.
     * If it is an [AppMenuMode], only the messages of the selected application will be deleted.
     */
    fun deleteAllMessages(menuMode: MenuMode) {
        viewModelScope.launch {
            when (menuMode) {
                is AppMenuMode -> repository.deleteAllMessages(menuMode.app)
                is AllAppMenuMode -> repository.deleteAllMessages()
            }
        }
    }

    /**
     * Deletes an application.
     * The application will be removed from the list of applications and the server.
     *
     * @param app The application to delete.
     */
    fun deleteApp(app: Application) {
        viewModelScope.launch {
            if (repository.deleteApp(app)) {
                onDeselectApplication()
            }
        }
    }

    /**
     * Loads more messages.
     * If the current application is null, messages from all applications will be loaded.
     * If the messages are currently being loaded, this method does nothing.
     */
    fun onLoadMore() {
        viewModelScope.launch {
            if (loadingMore.compareAndSet(false, true)) {
                currentApp.value?.let { repository.fetchMessages(it) } ?: repository.fetchMessages()
            }
        }
    }
}

/**
 * A class representing the menu mode should be displayed in the messages screen.
 */
sealed class MenuMode

/**
 * A class representing the menu mode should be displayed in the messages screen.
 */
data object AllAppMenuMode : MenuMode()

/**
 * A class representing the menu mode should be displayed in the messages screen.
 *
 * @param app The application to display the messages of.
 */
data class AppMenuMode(val app: Application) : MenuMode()
