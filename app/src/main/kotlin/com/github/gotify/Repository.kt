package com.github.gotify

import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.ApplicationApi
import com.github.gotify.client.api.MessageApi
import com.github.gotify.client.model.Application
import com.github.gotify.client.model.Message
import com.github.gotify.client.model.Paging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.awaitResponse

/**
 * A class that represents the repository.
 *
 * @property scope The coroutine scope where the repository will run.
 * @property baseUrl The base url of the server used to create the full url of the images.
 * @property applicationApi The application api used to interact with the server.
 * @property messageApi The message api used to interact with the server.
 */
class Repository
private constructor(
    private val scope: CoroutineScope,
    private val baseUrl: String,
    private val applicationApi: ApplicationApi,
    private val messageApi: MessageApi
) {
    private val applications = MutableStateFlow<List<Application>>(emptyList())
    private val messages = MutableStateFlow<Map<Long, List<Message>>>(emptyMap())
    private val paging = MutableStateFlow<Map<Long, PagingState>>(emptyMap())
    private var allApplicationsPaging: PagingState = PagingState()

    /**
     * Get all applications.
     * It returns a [StateFlow] object that emits the list of all applications with their states
     * whenever the list changes.
     */
    val applicationsState: StateFlow<List<ApplicationState>> =
        combine(applications, messages, paging) { apps, messages, paging ->
            apps.map { app ->
                val unreadCount = messages[app.id]?.size ?: 0
                val hasMoreMessages = paging[app.id]?.hasMore ?: true
                ApplicationState(
                    app,
                    unreadCount,
                    hasMoreMessages,
                    app.image?.let {
                        (
                            it.toHttpUrlOrNull() ?: baseUrl.toHttpUrlOrNull()?.newBuilder()
                                ?.addPathSegment(it)?.build()?.toString()
                            )?.toString()
                    }
                )
            }.also { println("JcLog: ${this@Repository} number of applications -> ${it.size}") }
        }.stateIn(scope = scope, started = SharingStarted.Lazily, initialValue = emptyList())

    /**
     * Get all messages of all applications.
     * It returns a [StateFlow] object that emits the list of all messages whenever the list changes.
     */
    val allMessages: StateFlow<List<Message>> =
        messages.map { it.values.flatten() }.distinctUntilChanged()
            .stateIn(scope = scope, started = SharingStarted.Lazily, initialValue = emptyList())

    /**
     * Get messages of an application.
     * It returns a [StateFlow] object that emits the list of messages of the application
     * whenever the list changes.
     *
     * @param application The application.
     *
     * @return The list of messages of the application.
     */
    fun getMessages(application: Application): StateFlow<List<Message>> =
        messages.map { it[application.id] ?: emptyList() }.distinctUntilChanged()
            .stateIn(scope = scope, started = SharingStarted.Lazily, initialValue = emptyList())

    /**
     * Fetch all applications.
     *
     * @return The list of the fetched applications.
     */
    suspend fun fetchApps(): List<Application> =
        applicationApi.getApps().awaitResponse().takeIf { it.isSuccessful }?.body()
            ?.also { applications.value = it } ?: emptyList()

    /**
     * Fetch messages from an application.
     *
     * @param application The application.
     * @param limit The number of messages to fetch.
     *
     * @return The list of the fetched messages.
     */
    suspend fun fetchMessages(application: Application, limit: Int = LIMIT): List<Message> {
        val currentPagingState = paging.value[application.id] ?: PagingState()
        return when (currentPagingState.hasMore) {
            false -> emptyList()
            true -> messageApi.getAppMessages(application.id, limit, currentPagingState.since)
                .awaitResponse().takeIf { it.isSuccessful }?.body()
                ?.also { paging.value += (application.id to it.paging.toPagingState()) }?.also {
                    println("JcLog: pagin.since -> ${it.paging.since}")
                    println("JcLog: pagin.next -> ${it.paging.next}")
                    println("JcLog: lastMessage -> ${it.messages.lastOrNull()?.id}")
                }?.also { storeMessages(it.messages, currentPagingState.since == 0L) }?.messages
                ?: emptyList()
        }
    }

    /**
     * Fetch messages from all applications.
     *
     * @param limit The number of messages to fetch.
     *
     * @return The list of the fetched messages.
     */
    suspend fun fetchMessages(limit: Int = LIMIT): List<Message> {
        return when (allApplicationsPaging.hasMore) {
            false -> return emptyList()
            true -> messageApi.getMessages(limit, allApplicationsPaging.since).awaitResponse()
                .takeIf { it.isSuccessful }?.body()
                ?.also { allApplicationsPaging = it.paging.toPagingState() }
                ?.also { storeMessages(it.messages, false) }?.messages ?: emptyList()
        }
    }

    /**
     * Delete a message.
     *
     * @param message The message to delete.
     *
     * @return A boolean value that indicates if the message was deleted.
     */
    suspend fun deleteMessage(message: Message): Boolean =
        messageApi.deleteMessage(message.id).awaitResponse().isSuccessful.also {
            if (it) {
                messages.value = messages.value.mapValues {
                    it.value.filterNot { it.id == message.id }
                }
            }
        }

    /**
     * Delete all messages of all applications.
     *
     * @return A boolean value that indicates if the messages were deleted.
     */
    suspend fun deleteAllMessages(): Boolean =
        messageApi.deleteMessages().awaitResponse().isSuccessful.also {
            if (it) messages.value = emptyMap()
        }

    /**
     * Delete all messages of an application.
     *
     * @param application The application.
     *
     * @return A boolean value that indicates if the messages were deleted.
     */
    suspend fun deleteAllMessages(application: Application): Boolean =
        messageApi.deleteAppMessages(application.id)
            .awaitResponse().isSuccessful.also { if (it) messages.value -= application.id }

    /**
     * Store a message in the repository.
     *
     * @param newMessage The message to store.
     */
    fun storeMessage(newMessage: Message) {
        storeMessages(listOf(newMessage), false)
    }

    /**
     * Store messages in the repository.
     *
     * @param newMessages The list of messages to store.
     * @param overrideOld A boolean value that indicates if the old messages should be overridden.
     */
    private fun storeMessages(newMessages: List<Message>, overrideOld: Boolean) {
        fetchMissingApplications(newMessages)
        val previousMessages = messages.value.takeUnless { overrideOld } ?: emptyMap()
        val mappedMessages: Map<Long, List<Message>> =
            newMessages.groupBy { it.appid }.map { (appId, messages) ->
                val previous = previousMessages[appId] ?: emptyList()
                (
                    appId to (
                        (
                            previous.associateBy {
                                it.id
                            } + messages.associateBy {
                                it.id
                            }
                            ).values.sortedByDescending {
                            it.id
                        }
                        )
                    )
            }.toMap()
        messages.value += mappedMessages
    }

    /**
     * Fetch applications that are missing from the list of messages.
     *
     * @param messages The list of messages.
     */
    private fun fetchMissingApplications(messages: List<Message>) {
        scope.launch {
            val appIds = applications.value.map { it.id }
            this@Repository.takeUnless { messages.all { appIds.contains(it.id) } }?.fetchApps()
        }
    }

    /**
     * Refresh all messages of all applications.
     *
     * @return The list of refreshed messages.
     */
    suspend fun refreshAllMessages() {
        applications.value.forEach { app ->
            refreshMessages(app)
        }
        allApplicationsPaging = PagingState()
        fetchMessages()
    }

    /**
     * Refresh the messages of an application.
     *
     * @param application The application to refresh.
     *
     * @return The list of refreshed messages.
     */
    suspend fun refreshMessages(application: Application): List<Message> {
        paging.value -= application.id
        return fetchMessages(application, messages.value[application.id]?.size ?: LIMIT)
    }

    /**
     * Initialize the repository. Fetch all applications and their last messages.
     */
    private suspend fun initialize() {
        fetchApps().forEach { app ->
            fetchMessages(app)
        }
        fetchMessages()
    }

    /**
     * Delete an application.
     *
     * @param app The application to delete.
     *
     * @return A boolean value that indicates if the application was deleted.
     */
    suspend fun deleteApp(app: Application): Boolean = applicationApi.deleteApp(app.id)
        .awaitResponse().isSuccessful.also { if (it) applications.value -= app }

    companion object {

        private const val LIMIT = 20

        /**
         * Create a new instance of the repository. The repository will be initialized after creation.
         *
         * @param scope The coroutine scope.
         * @param settings The settings.
         */
        internal fun create(scope: CoroutineScope, settings: Settings): Repository {
            val apiClient = ClientFactory.clientToken(settings)
            val applicationApi = apiClient.createService(ApplicationApi::class.java)
            val messageApi = apiClient.createService(MessageApi::class.java)
            return Repository(scope, settings.url, applicationApi, messageApi).also {
                scope.launch { it.initialize() }
            }
        }
    }
}

/**
 * A function that converts a [Paging] object to a [PagingState] object.
 */
private fun Paging.toPagingState(): PagingState = PagingState(since, next != null)

/**
 * A class that represents the state of the paging.
 *
 * @property since The older message id value of the paging.
 * @property hasMore A boolean value that indicates if there are more messages.
 */
private data class PagingState(
    val since: Long = 0,
    val hasMore: Boolean = true
)

/**
 * A class that represents the state of the application.
 *
 * @property application The application.
 * @property unreadCount The number of unread messages.
 * @property hasMoreMessages A boolean value that indicates if there are more messages.
 * @property iconUrl The icon url of the application.
 */
data class ApplicationState(
    val application: Application,
    val unreadCount: Int,
    val hasMoreMessages: Boolean,
    val iconUrl: String?
)
