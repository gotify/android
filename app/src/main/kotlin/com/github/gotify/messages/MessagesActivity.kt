package com.github.gotify.messages

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.request.ImageRequest
import com.github.gotify.BuildConfig
import com.github.gotify.CoilInstance
import com.github.gotify.MissedMessageUtil
import com.github.gotify.R
import com.github.gotify.Utils
import com.github.gotify.Utils.launchCoroutine
import com.github.gotify.api.Api
import com.github.gotify.api.ApiException
import com.github.gotify.api.Callback
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.ApplicationApi
import com.github.gotify.client.api.ClientApi
import com.github.gotify.client.api.MessageApi
import com.github.gotify.client.model.Application
import com.github.gotify.client.model.Client
import com.github.gotify.client.model.Message
import com.github.gotify.databinding.ActivityMessagesBinding
import com.github.gotify.init.InitializationActivity
import com.github.gotify.log.LogsActivity
import com.github.gotify.login.LoginActivity
import com.github.gotify.messages.provider.MessageState
import com.github.gotify.messages.provider.MessageWithImage
import com.github.gotify.service.WebSocketService
import com.github.gotify.settings.SettingsActivity
import com.github.gotify.sharing.ShareActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tinylog.kotlin.Logger

internal class MessagesActivity :
    AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMessagesBinding
    private lateinit var viewModel: MessagesModel
    private var isLoadMore = false
    private var updateAppOnDrawerClose: Long? = null
    private lateinit var listMessageAdapter: ListMessageAdapter
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val messageJson = intent.getStringExtra("message")
            val message = Utils.JSON.fromJson(
                messageJson,
                Message::class.java
            )
            launchCoroutine {
                addSingleMessage(message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this, MessagesModelFactory(this))[MessagesModel::class.java]
        Logger.info("Entering " + javaClass.simpleName)
        initDrawer()

        val layoutManager = LinearLayoutManager(this)
        val messagesView: RecyclerView = binding.messagesView
        val dividerItemDecoration = DividerItemDecoration(
            messagesView.context,
            layoutManager.orientation
        )
        listMessageAdapter = ListMessageAdapter(
            this,
            viewModel.settings,
            CoilInstance.get(this)
        ) { message ->
            scheduleDeletion(message)
        }
        addBackPressCallback()

        messagesView.addItemDecoration(dividerItemDecoration)
        messagesView.setHasFixedSize(true)
        messagesView.layoutManager = layoutManager
        messagesView.addOnScrollListener(MessageListOnScrollListener())
        messagesView.adapter = listMessageAdapter

        val appsHolder = viewModel.appsHolder
        appsHolder.onUpdate { onUpdateApps(appsHolder.get()) }
        if (appsHolder.wasRequested()) onUpdateApps(appsHolder.get()) else appsHolder.request()

        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(listMessageAdapter))
        itemTouchHelper.attachToRecyclerView(messagesView)

        val swipeRefreshLayout = binding.swipeRefresh
        swipeRefreshLayout.setOnRefreshListener { onRefresh() }
        binding.drawerLayout.addDrawerListener(
            object : SimpleDrawerListener() {
                override fun onDrawerOpened(drawerView: View) {
                    onBackPressedCallback.isEnabled = true
                }

                override fun onDrawerClosed(drawerView: View) {
                    updateAppOnDrawerClose?.let { selectApp ->
                        updateAppOnDrawerClose = null
                        viewModel.appId = selectApp
                        launchCoroutine {
                            updateMessagesForApplication(true, selectApp)
                        }
                        invalidateOptionsMenu()
                    }
                    onBackPressedCallback.isEnabled = false
                }
            }
        )

        swipeRefreshLayout.isEnabled = false
        messagesView
            .viewTreeObserver
            .addOnScrollChangedListener {
                val topChild = messagesView.getChildAt(0)
                if (topChild != null) {
                    swipeRefreshLayout.isEnabled = topChild.top == 0
                } else {
                    swipeRefreshLayout.isEnabled = true
                }
            }

        val excludeFromRecent = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(getString(R.string.setting_key_exclude_from_recent), false)
        Utils.setExcludeFromRecent(this, excludeFromRecent)
        launchCoroutine {
            updateMessagesForApplication(true, viewModel.appId)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        binding.learnGotify.setOnClickListener { openDocumentation() }
    }

    private fun refreshAll() {
        CoilInstance.evict(this)
        startActivity(Intent(this, InitializationActivity::class.java))
        finish()
    }

    private fun onRefresh() {
        CoilInstance.evict(this)
        viewModel.messages.clear()
        launchCoroutine {
            loadMore(viewModel.appId).forEachIndexed { index, message ->
                if (message.image != null) {
                    listMessageAdapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun openDocumentation() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gotify.net/docs/pushmsg"))
        startActivity(browserIntent)
    }

    private fun onUpdateApps(applications: List<Application>) {
        val menu: Menu = binding.navView.menu
        menu.removeGroup(R.id.apps)
        viewModel.targetReferences.clear()
        updateMessagesAndStopLoading(viewModel.messages[viewModel.appId])
        var selectedItem = menu.findItem(R.id.nav_all_messages)
        applications.indices.forEach { index ->
            val app = applications[index]
            val item = menu.add(R.id.apps, index, APPLICATION_ORDER, app.name)
            item.isCheckable = true
            if (app.id == viewModel.appId) selectedItem = item
            val t = Utils.toDrawable { icon -> item.icon = icon }
            viewModel.targetReferences.add(t)
            val request = ImageRequest.Builder(this)
                .data(Utils.resolveAbsoluteUrl(viewModel.settings.url + "/", app.image))
                .error(R.drawable.ic_alarm)
                .placeholder(R.drawable.ic_placeholder)
                .size(100, 100)
                .target(t)
                .build()
            CoilInstance.get(this).enqueue(request)
        }
        selectAppInMenu(selectedItem)
    }

    private fun initDrawer() {
        setSupportActionBar(binding.appBarDrawer.toolbar)
        binding.navView.itemIconTintList = null
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.appBarDrawer.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
        val headerView = binding.navView.getHeaderView(0)

        val settings = viewModel.settings

        val user = headerView.findViewById<TextView>(R.id.header_user)
        user.text = settings.user?.name

        val connection = headerView.findViewById<TextView>(R.id.header_connection)
        connection.text = settings.url

        val version = headerView.findViewById<TextView>(R.id.header_version)
        version.text =
            getString(R.string.versions, BuildConfig.VERSION_NAME, settings.serverVersion)

        val refreshAll = headerView.findViewById<ImageButton>(R.id.refresh_all)
        refreshAll.setOnClickListener { refreshAll() }
    }

    private fun addBackPressCallback() {
        onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        if (item.groupId == R.id.apps) {
            val app = viewModel.appsHolder.get()[id]
            updateAppOnDrawerClose = app.id
            startLoading()
            binding.appBarDrawer.toolbar.subtitle = item.title
        } else if (id == R.id.nav_all_messages) {
            updateAppOnDrawerClose = MessageState.ALL_MESSAGES
            startLoading()
            binding.appBarDrawer.toolbar.subtitle = ""
        } else if (id == R.id.logout) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout)
                .setMessage(getString(R.string.logout_confirm))
                .setPositiveButton(R.string.yes) { _, _ -> doLogout() }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else if (id == R.id.nav_logs) {
            startActivity(Intent(this, LogsActivity::class.java))
        } else if (id == R.id.settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
        } else if (id == R.id.push_message) {
            val intent = Intent(this@MessagesActivity, ShareActivity::class.java)
            startActivity(intent)
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun doLogout() {
        setContentView(R.layout.splash)
        launchCoroutine {
            deleteClientAndNavigateToLogin()
        }
    }

    private fun startLoading() {
        binding.swipeRefresh.isRefreshing = true
        binding.messagesView.visibility = View.GONE
    }

    private fun stopLoading() {
        binding.swipeRefresh.isRefreshing = false
        binding.messagesView.visibility = View.VISIBLE
    }

    override fun onResume() {
        val context = applicationContext
        val nManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nManager.cancelAll()
        val filter = IntentFilter()
        filter.addAction(WebSocketService.NEW_MESSAGE_BROADCAST)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        launchCoroutine {
            updateMissedMessages(viewModel.messages.getLastReceivedMessage())
        }
        var selectedIndex = R.id.nav_all_messages
        val appId = viewModel.appId
        if (appId != MessageState.ALL_MESSAGES) {
            val apps = viewModel.appsHolder.get()
            apps.indices.forEach { index ->
                if (apps[index].id == appId) {
                    selectedIndex = index
                }
            }
        }
        // Force re-render of all items to update relative date-times on app resume.
        listMessageAdapter.notifyDataSetChanged()
        selectAppInMenu(binding.navView.menu.findItem(selectedIndex))
        super.onResume()
    }

    override fun onPause() {
        unregisterReceiver(receiver)
        super.onPause()
    }

    private fun selectAppInMenu(appItem: MenuItem?) {
        if (appItem != null) {
            appItem.isChecked = true
            if (appItem.itemId != R.id.nav_all_messages) {
                binding.appBarDrawer.toolbar.subtitle = appItem.title
            }
        }
    }

    private fun scheduleDeletion(message: Message) {
        val adapter = binding.messagesView.adapter as ListMessageAdapter
        val messages = viewModel.messages
        messages.deleteLocal(message)
        adapter.updateList(messages[viewModel.appId])
        showDeletionSnackbar()
    }

    private fun undoDelete() {
        val messages = viewModel.messages
        val deletion = messages.undoDeleteLocal()
        if (deletion != null) {
            val adapter = binding.messagesView.adapter as ListMessageAdapter
            val appId = viewModel.appId
            adapter.updateList(messages[appId])
        }
    }

    private fun showDeletionSnackbar() {
        val view: View = binding.swipeRefresh
        val snackbar = Snackbar.make(view, R.string.snackbar_deleted, Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.snackbar_undo) { undoDelete() }
        snackbar.addCallback(SnackbarCallback())
        snackbar.show()
    }

    private inner class SnackbarCallback : BaseCallback<Snackbar?>() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            super.onDismissed(transientBottomBar, event)
            if (event != DISMISS_EVENT_ACTION && event != DISMISS_EVENT_CONSECUTIVE) {
                // Execute deletion when the snackbar disappeared without pressing the undo button
                // DISMISS_EVENT_CONSECUTIVE should be excluded as well, because it would cause the
                // deletion to be sent to the server twice, since the deletion is sent to the server
                // in MessageFacade if a message is deleted while another message was already
                // waiting for deletion.
                launchCoroutine {
                    commitDeleteMessage()
                }
            }
        }
    }

    private inner class SwipeToDeleteCallback(
        private val adapter: ListMessageAdapter
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        private var icon: Drawable?
        private val background: ColorDrawable

        init {
            val backgroundColorId =
                ContextCompat.getColor(this@MessagesActivity, R.color.swipeBackground)
            val iconColorId = ContextCompat.getColor(this@MessagesActivity, R.color.swipeIcon)
            val drawable = ContextCompat.getDrawable(this@MessagesActivity, R.drawable.ic_delete)
            icon = null
            if (drawable != null) {
                icon = DrawableCompat.wrap(drawable.mutate())
                DrawableCompat.setTint(icon!!, iconColorId)
            }
            background = ColorDrawable(backgroundColorId)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val message = adapter.currentList[position]
            scheduleDeletion(message.message)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            icon?.let {
                val itemView = viewHolder.itemView
                val iconHeight = itemView.height / 3
                val scale = iconHeight / it.intrinsicHeight.toDouble()
                val iconWidth = (it.intrinsicWidth * scale).toInt()
                val iconMarginLeftRight = 50
                val iconMarginTopBottom = (itemView.height - iconHeight) / 2
                val iconTop = itemView.top + iconMarginTopBottom
                val iconBottom = itemView.bottom - iconMarginTopBottom
                if (dX > 0) {
                    // Swiping to the right
                    val iconLeft = itemView.left + iconMarginLeftRight
                    val iconRight = itemView.left + iconMarginLeftRight + iconWidth
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                } else if (dX < 0) {
                    // Swiping to the left
                    val iconLeft = itemView.right - iconMarginLeftRight - iconWidth
                    val iconRight = itemView.right - iconMarginLeftRight
                    it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                } else {
                    // View is unswiped
                    it.setBounds(0, 0, 0, 0)
                    background.setBounds(0, 0, 0, 0)
                }
                background.draw(c)
                it.draw(c)
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private inner class MessageListOnScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {}

        override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
            val linearLayoutManager = view.layoutManager as LinearLayoutManager?
            if (linearLayoutManager != null) {
                val lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition()
                val totalItemCount = view.adapter!!.itemCount
                if (lastVisibleItem > totalItemCount - 15 &&
                    totalItemCount != 0 &&
                    viewModel.messages.canLoadMore(viewModel.appId)
                ) {
                    if (!isLoadMore) {
                        isLoadMore = true
                        launchCoroutine {
                            loadMore(viewModel.appId)
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateMissedMessages(id: Long) {
        if (id == -1L) return

        val newMessages = MissedMessageUtil(viewModel.client.createService(MessageApi::class.java))
            .missingMessages(id).filterNotNull()
        viewModel.messages.addMessages(newMessages)

        if (newMessages.isNotEmpty()) {
            updateMessagesForApplication(true, viewModel.appId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.messages_action, menu)
        menu.findItem(R.id.action_delete_app).isVisible =
            viewModel.appId != MessageState.ALL_MESSAGES
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete_all) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_all)
                .setMessage(R.string.ack)
                .setPositiveButton(R.string.yes) { _, _ ->
                    launchCoroutine {
                        deleteMessages(viewModel.appId)
                    }
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
        if (item.itemId == R.id.action_delete_app) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_app)
                .setMessage(R.string.ack)
                .setPositiveButton(R.string.yes) { _, _ -> deleteApp(viewModel.appId) }
                .setNegativeButton(R.string.no, null)
                .show()
        }
        return super.onContextItemSelected(item)
    }

    private fun deleteApp(appId: Long) {
        val settings = viewModel.settings
        val client = ClientFactory.clientToken(settings)
        client.createService(ApplicationApi::class.java)
            .deleteApp(appId)
            .enqueue(
                Callback.callInUI(
                    this,
                    onSuccess = { refreshAll() },
                    onError = { Utils.showSnackBar(this, getString(R.string.error_delete_app)) }
                )
            )
    }

    private suspend fun loadMore(appId: Long): List<MessageWithImage> {
        val messagesWithImages = viewModel.messages.loadMore(appId)
        withContext(Dispatchers.Main) {
            updateMessagesAndStopLoading(messagesWithImages)
        }
        return messagesWithImages
    }

    private suspend fun updateMessagesForApplication(withLoadingSpinner: Boolean, appId: Long) {
        if (withLoadingSpinner) {
            withContext(Dispatchers.Main) {
                startLoading()
            }
        }
        viewModel.messages.loadMoreIfNotPresent(appId)
        withContext(Dispatchers.Main) {
            updateMessagesAndStopLoading(viewModel.messages[appId])
        }
    }

    private suspend fun addSingleMessage(message: Message) {
        viewModel.messages.addMessages(listOf(message))
        updateMessagesForApplication(false, viewModel.appId)
    }

    private suspend fun commitDeleteMessage() {
        viewModel.messages.commitDelete()
        updateMessagesForApplication(false, viewModel.appId)
    }

    private suspend fun deleteMessages(appId: Long) {
        withContext(Dispatchers.Main) {
            startLoading()
        }
        val success = viewModel.messages.deleteAll(appId)
        if (success) {
            updateMessagesForApplication(false, viewModel.appId)
        } else {
            withContext(Dispatchers.Main) {
                Utils.showSnackBar(this@MessagesActivity, "Delete failed :(")
            }
        }
    }

    private fun deleteClientAndNavigateToLogin() {
        val settings = viewModel.settings
        val api = ClientFactory.clientToken(settings).createService(ClientApi::class.java)
        stopService(Intent(this@MessagesActivity, WebSocketService::class.java))
        try {
            val clients = Api.execute(api.clients)
            var currentClient: Client? = null
            for (client in clients) {
                if (client.token == settings.token) {
                    currentClient = client
                    break
                }
            }
            if (currentClient != null) {
                Logger.info("Delete client with id " + currentClient.id)
                Api.execute(api.deleteClient(currentClient.id))
            } else {
                Logger.error("Could not delete client, client does not exist.")
            }
        } catch (e: ApiException) {
            Logger.error(e, "Could not delete client")
        }

        viewModel.settings.clear()
        startActivity(Intent(this@MessagesActivity, LoginActivity::class.java))
        finish()
    }

    private fun updateMessagesAndStopLoading(messageWithImages: List<MessageWithImage>) {
        isLoadMore = false
        stopLoading()
        if (messageWithImages.isEmpty()) {
            binding.flipper.displayedChild = 1
        } else {
            binding.flipper.displayedChild = 0
        }
        val adapter = binding.messagesView.adapter as ListMessageAdapter
        adapter.updateList(messageWithImages)
    }

    private fun ListMessageAdapter.updateList(list: List<MessageWithImage>) {
        this.submitList(if (this.currentList == list) list.toList() else list) {
            val topChild = binding.messagesView.getChildAt(0)
            if (topChild != null && topChild.top == 0) {
                binding.messagesView.scrollToPosition(0)
            }
        }
    }

    companion object {
        private const val APPLICATION_ORDER = 1
    }
}
