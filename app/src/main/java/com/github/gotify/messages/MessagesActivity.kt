package com.github.gotify.messages

import android.app.NotificationManager
import android.content.*
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.gotify.BuildConfig
import com.github.gotify.MissedMessageUtil
import com.github.gotify.R
import com.github.gotify.Utils
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
import com.github.gotify.log.Log
import com.github.gotify.log.LogsActivity
import com.github.gotify.login.LoginActivity
import com.github.gotify.messages.ListMessageAdapter.Delete
import com.github.gotify.messages.provider.*
import com.github.gotify.service.WebSocketService
import com.github.gotify.service.WebSocketService.Companion.NEW_MESSAGE_BROADCAST
import com.github.gotify.settings.SettingsActivity
import com.github.gotify.sharing.ShareActivity
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.BaseTransientBottomBar.BaseCallback
import com.google.android.material.snackbar.Snackbar
import java.io.IOException

class MessagesActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var binding: ActivityMessagesBinding
    private lateinit var viewModel: MessagesModel
    private var isLoadMore = false
    private var updateAppOnDrawerClose: Long? = null
    private lateinit var listMessageAdapter: ListMessageAdapter

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val messageJson = intent.getStringExtra("message")
            val message = Utils.JSON.fromJson(
                messageJson,
                Message::class.java
            )
            NewSingleMessage().execute(message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this, MessagesModelFactory(this))
            .get(MessagesModel::class.java)
        Log.i("Entering " + javaClass.simpleName)
        initDrawer()
        val layoutManager = LinearLayoutManager(this)
        val messagesView: RecyclerView = binding.messagesView
        val dividerItemDecoration = DividerItemDecoration(
            messagesView.context, layoutManager.orientation
        )
        listMessageAdapter = ListMessageAdapter(
            this,
            viewModel.settings,
            viewModel.picassoHandler.get(),
            emptyList(),
            object : Delete {
                override fun delete(position: Int, message: Message, listAnimation: Boolean) {
                    scheduleDeletion(
                        position,
                        message,
                        listAnimation
                    )
                }
            })
        messagesView.addItemDecoration(dividerItemDecoration)
        messagesView.setHasFixedSize(true)
        messagesView.layoutManager = layoutManager
        messagesView.addOnScrollListener(MessageListOnScrollListener())
        messagesView.adapter = listMessageAdapter
        val appsHolder = viewModel.appsHolder
        appsHolder.onUpdate { onUpdateApps(appsHolder.get()) }
        if (appsHolder.wasRequested()) onUpdateApps(appsHolder.get()) else appsHolder.request()
        val itemTouchHelper = ItemTouchHelper(
            SwipeToDeleteCallback(
                listMessageAdapter
            )
        )
        itemTouchHelper.attachToRecyclerView(messagesView)
        val swipeRefreshLayout: SwipeRefreshLayout = binding.swipeRefresh
        swipeRefreshLayout.setOnRefreshListener { onRefresh() }
        binding.drawerLayout.addDrawerListener(
            object : SimpleDrawerListener() {
                override fun onDrawerClosed(drawerView: View) {
                    if (updateAppOnDrawerClose != null) {
                        viewModel.appId = updateAppOnDrawerClose!!
                        UpdateMessagesForApplication(true).execute(updateAppOnDrawerClose)
                        updateAppOnDrawerClose = null
                        invalidateOptionsMenu()
                    }
                }
            })
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
        UpdateMessagesForApplication(true).execute(viewModel.appId)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        binding.learnGotify.setOnClickListener { openDocumentation() }
    }

    fun onRefreshAll(view: View?) {
        refreshAll()
    }

    fun refreshAll() {
        try {
            viewModel.picassoHandler.evict()
        } catch (e: IOException) {
            Log.e("Problem evicting Picasso cache", e)
        }
        startActivity(Intent(this, InitializationActivity::class.java))
        finish()
    }

    private fun onRefresh() {
        viewModel.messages.clear()
        LoadMore().execute(viewModel.appId)
    }

    private fun openDocumentation() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gotify.net/docs/pushmsg"))
        startActivity(browserIntent)
    }

    fun commitDelete() {
        CommitDeleteMessage().execute()
    }

    protected fun onUpdateApps(applications: List<Application>) {
        val menu: Menu = binding.navView.menu
        menu.removeGroup(R.id.apps)
        viewModel.targetReferences.clear()
        updateMessagesAndStopLoading(viewModel.messages[viewModel.appId])
        var selectedItem = menu.findItem(R.id.nav_all_messages)
        for (i in applications.indices) {
            val app = applications[i]
            val item = menu.add(R.id.apps, i, APPLICATION_ORDER, app.name)
            item.isCheckable = true
            if (app.id == viewModel.appId) selectedItem = item
            val t = Utils.toDrawable(
                resources
            ) { icon: Drawable? -> item.icon = icon }
            viewModel.targetReferences.add(t)
            viewModel
                .picassoHandler
                .get()
                .load(
                    Utils.resolveAbsoluteUrl(
                        viewModel.settings.url() + "/", app.image
                    )
                )
                .error(R.drawable.ic_alarm)
                .placeholder(R.drawable.ic_placeholder)
                .resize(100, 100)
                .into(t)
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
        val headerView: View = binding.navView.getHeaderView(0)
        val settings = viewModel.settings
        val user = headerView.findViewById<TextView>(R.id.header_user)
        user.text = settings.user().name
        val connection = headerView.findViewById<TextView>(R.id.header_connection)
        connection.text = getString(R.string.connection, settings.user().name, settings.url())
        val version = headerView.findViewById<TextView>(R.id.header_version)
        version.text =
            getString(R.string.versions, BuildConfig.VERSION_NAME, settings.serverVersion())
        val refreshAll = headerView.findViewById<ImageButton>(R.id.refresh_all)
        refreshAll.setOnClickListener { view: View? ->
            onRefreshAll(
                view
            )
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        if (item.groupId == R.id.apps) {
            val app = viewModel.appsHolder.get()[id]
            updateAppOnDrawerClose = if (app != null) app.id else MessageState.ALL_MESSAGES
            startLoading()
            binding.appBarDrawer.toolbar.subtitle = item.title
        } else if (id == R.id.nav_all_messages) {
            updateAppOnDrawerClose = MessageState.ALL_MESSAGES
            startLoading()
            binding.appBarDrawer.toolbar.subtitle = ""
        } else if (id == R.id.logout) {
            AlertDialog.Builder(ContextThemeWrapper(this, R.style.AppTheme_Dialog))
                .setTitle(R.string.logout)
                .setMessage(getString(R.string.logout_confirm))
                .setPositiveButton(R.string.yes) { _, _ ->
                    doLogout()
                }
                .setNegativeButton(R.string.cancel) { a, b -> }
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

    fun doLogout() {
        setContentView(R.layout.splash)
        DeleteClientAndNavigateToLogin().execute()
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
        filter.addAction(NEW_MESSAGE_BROADCAST)
        registerReceiver(receiver, filter)
        UpdateMissedMessages().execute(viewModel.messages.getLastReceivedMessage())
        var selectedIndex: Int = R.id.nav_all_messages
        val appId = viewModel.appId
        if (appId != MessageState.ALL_MESSAGES) {
            val apps = viewModel.appsHolder.get()
            for (i in apps.indices) {
                if (apps[i].id == appId) {
                    selectedIndex = i
                }
            }
        }
        listMessageAdapter!!.notifyDataSetChanged()
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
            if (appItem.itemId != R.id.nav_all_messages) binding.appBarDrawer.toolbar.subtitle =
                appItem.title
        }
    }

    private fun scheduleDeletion(position: Int, message: Message, listAnimation: Boolean) {
        val adapter = binding.messagesView.adapter as ListMessageAdapter
        val messages = viewModel.messages
        messages.deleteLocal(message)
        adapter.items = messages[viewModel.appId]
        if (listAnimation) adapter.notifyItemRemoved(position) else adapter.notifyDataSetChanged()
        showDeletionSnackbar()
    }

    private fun undoDelete() {
        val messages = viewModel.messages
        val deletion = messages.undoDeleteLocal()
        if (deletion != null) {
            val adapter = binding.messagesView.adapter as ListMessageAdapter
            val appId = viewModel.appId
            adapter.items = messages[appId]
            val insertPosition =
                if (appId == MessageState.ALL_MESSAGES) deletion.allPosition else deletion.appPosition
            adapter.notifyItemInserted(insertPosition)
        }
    }

    private fun showDeletionSnackbar() {
        val view: View = binding.swipeRefresh
        val snackbar: Snackbar =
            Snackbar.make(view, R.string.snackbar_deleted, Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.snackbar_undo) { v -> undoDelete() }
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
                commitDelete()
            }
        }
    }

    private inner class SwipeToDeleteCallback(private val adapter: ListMessageAdapter) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
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
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val message = adapter.items[position]
            scheduleDeletion(position, message.message, true)
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
            if (icon != null) {
                val itemView = viewHolder.itemView
                val iconHeight = itemView.height / 3
                val scale = iconHeight / icon!!.intrinsicHeight.toDouble()
                val iconWidth = (icon!!.intrinsicWidth * scale).toInt()
                val iconMarginLeftRight = 50
                val iconMarginTopBottom = (itemView.height - iconHeight) / 2
                val iconTop = itemView.top + iconMarginTopBottom
                val iconBottom = itemView.bottom - iconMarginTopBottom
                if (dX > 0) {
                    // Swiping to the right
                    val iconLeft = itemView.left + iconMarginLeftRight
                    val iconRight = itemView.left + iconMarginLeftRight + iconWidth
                    icon!!.setBounds(iconLeft, iconTop, iconRight, iconBottom)
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
                    icon!!.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                } else {
                    // View is unswiped
                    icon!!.setBounds(0, 0, 0, 0)
                    background.setBounds(0, 0, 0, 0)
                }
                background.draw(c)
                icon!!.draw(c)
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
                if (lastVisibleItem > totalItemCount - 15 && totalItemCount != 0 && viewModel.messages.canLoadMore(
                        viewModel.appId
                    )
                ) {
                    if (!isLoadMore) {
                        isLoadMore = true
                        LoadMore().execute(viewModel.appId)
                    }
                }
            }
        }
    }

    private inner class UpdateMissedMessages : AsyncTask<Long?, Void?, Boolean>() {
        override fun doInBackground(vararg ids: Long?): Boolean {
            val id = Utils.first<Long>(ids)
            if (id == -1L) {
                return false
            }
            val newMessages = MissedMessageUtil(
                viewModel.client.createService(
                    MessageApi::class.java
                )
            )
                .missingMessages(id)
            viewModel.messages.addMessages(newMessages)
            return newMessages.isNotEmpty()
        }

        override fun onPostExecute(update: Boolean) {
            if (update) {
                UpdateMessagesForApplication(true).execute(viewModel.appId)
            }
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
            DeleteMessages().execute(viewModel.appId)
        }
        if (item.itemId == R.id.action_delete_app) {
            val alert = android.app.AlertDialog.Builder(this)
            alert.setTitle(R.string.delete_app)
            alert.setMessage(R.string.ack)
            alert.setPositiveButton(
                R.string.yes
            ) { _, _ -> deleteApp(viewModel.appId) }
            alert.setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
            alert.show()
        }
        return super.onContextItemSelected(item)
    }

    private fun deleteApp(appId: Long) {
        val settings = viewModel.settings
        val client =
            ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token())
        client.createService(ApplicationApi::class.java)
            .deleteApp(appId)
            .enqueue(
                Callback.callInUI(
                    this,
                    { refreshAll() }
                ) {
                    Utils.showSnackBar(
                        this, getString(R.string.error_delete_app)
                    )
                })
    }

    private inner class LoadMore : AsyncTask<Long?, Void?, List<MessageWithImage>>() {
        override fun doInBackground(vararg appId: Long?): List<MessageWithImage> {
            return viewModel.messages.loadMore(appId.first()!!)
        }

        override fun onPostExecute(messageWithImages: List<MessageWithImage>) {
            updateMessagesAndStopLoading(messageWithImages)
        }
    }

    private inner class UpdateMessagesForApplication(withLoadingSpinner: Boolean) :
        AsyncTask<Long?, Void?, Long>() {
        init {
            if (withLoadingSpinner) {
                startLoading()
            }
        }

        override fun doInBackground(vararg appIds: Long?): Long {
            val appId = Utils.first<Long>(appIds)
            viewModel.messages.loadMoreIfNotPresent(appId)
            return appId
        }

        override fun onPostExecute(appId: Long) {
            updateMessagesAndStopLoading(viewModel.messages[appId])
        }
    }

    private inner class NewSingleMessage : AsyncTask<Message?, Void?, Void?>() {
        override fun doInBackground(vararg newMessages: Message?): Void? {
            viewModel.messages.addMessages(listOfNotNull(*newMessages))
            return null
        }

        override fun onPostExecute(data: Void?) {
            UpdateMessagesForApplication(false).execute(viewModel.appId)
        }
    }

    private inner class CommitDeleteMessage : AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg messages: Void?): Void? {
            viewModel.messages.commitDelete()
            return null
        }

        override fun onPostExecute(data: Void?) {
            UpdateMessagesForApplication(false).execute(viewModel.appId)
        }
    }

    private inner class DeleteMessages : AsyncTask<Long?, Void?, Boolean>() {
        init {
            startLoading()
        }

        override fun doInBackground(vararg appId: Long?): Boolean {
            return viewModel.messages.deleteAll(appId.first()!!)
        }

        override fun onPostExecute(success: Boolean) {
            if (!success) {
                Utils.showSnackBar(this@MessagesActivity, "Delete failed :(")
            }
            UpdateMessagesForApplication(false).execute(viewModel.appId)
        }
    }

    private inner class DeleteClientAndNavigateToLogin :
        AsyncTask<Void?, Void?, Void?>() {
        override fun doInBackground(vararg ignore: Void?): Void? {
            val settings = viewModel.settings
            val api = ClientFactory.clientToken(
                settings.url(), settings.sslSettings(), settings.token()
            )
                .createService(ClientApi::class.java)
            stopService(Intent(this@MessagesActivity, WebSocketService::class.java))
            try {
                val clients = Api.execute(api.clients)
                var currentClient: Client? = null
                for (client in clients) {
                    if (client.token == settings.token()) {
                        currentClient = client
                        break
                    }
                }
                if (currentClient != null) {
                    Log.i("Delete client with id " + currentClient.id)
                    Api.execute(api.deleteClient(currentClient.id))
                } else {
                    Log.e("Could not delete client, client does not exist.")
                }
            } catch (e: ApiException) {
                Log.e("Could not delete client", e)
            }
            return null
        }

        override fun onPostExecute(aVoid: Void?) {
            viewModel.settings.clear()
            startActivity(Intent(this@MessagesActivity, LoginActivity::class.java))
            finish()
            super.onPostExecute(aVoid)
        }
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
        adapter.items = messageWithImages
        adapter.notifyDataSetChanged()
    }

    companion object {
        private const val APPLICATION_ORDER = 1
    }
}
