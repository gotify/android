package com.github.gotify.messages

import android.app.NotificationManager
import android.content.Intent
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.request.ImageRequest
import com.github.gotify.ApplicationState
import com.github.gotify.BuildConfig
import com.github.gotify.CoilInstance
import com.github.gotify.R
import com.github.gotify.Repository
import com.github.gotify.Settings
import com.github.gotify.Utils
import com.github.gotify.Utils.launchCoroutine
import com.github.gotify.api.Api
import com.github.gotify.api.ApiException
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.ClientApi
import com.github.gotify.client.model.Client
import com.github.gotify.client.model.Message
import com.github.gotify.databinding.ActivityMessagesBinding
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
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.tinylog.kotlin.Logger

internal class MessagesActivity :
    AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {
    private val binding: ActivityMessagesBinding by lazy {
        ActivityMessagesBinding.inflate(layoutInflater)
    }
    private val settings: Settings by lazy {
        Settings(this)
    }
    private val messagesViewModel: MessagesViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MessagesViewModel(
                Repository.create(
                    scope = lifecycleScope,
                    settings = settings
                ).also { WebSocketService.repository = it }
            ) as T
        }
    }
    private var updateAppOnDrawerClose: Long? = null
    private val listMessageAdapter: ListMessageAdapter by lazy {
        ListMessageAdapter(
            this,
            settings,
            CoilInstance.get(this)
        ) { message ->
            shadowDelete(message)
        }
    }
    private val onBackPressedCallback: OnBackPressedCallback by lazy {
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
            }
        }.also {
            onBackPressedDispatcher.addCallback(this, it)
        }
    }
    private val swipeRefreshLayout by lazy {
        binding.swipeRefresh
    }
    private var menuMode: MenuMode = AllAppMenuMode
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        Logger.info("Entering " + javaClass.simpleName)
        initDrawer()

        val layoutManager = LinearLayoutManager(this)
        val messagesView: RecyclerView = binding.messagesView
        val dividerItemDecoration = DividerItemDecoration(
            messagesView.context,
            layoutManager.orientation
        )

        bindViewModel()

        messagesView.addItemDecoration(dividerItemDecoration)
        messagesView.setHasFixedSize(true)
        messagesView.layoutManager = layoutManager
        messagesView.addOnScrollListener(MessageListOnScrollListener())
        messagesView.adapter = listMessageAdapter

        val itemTouchHelper = ItemTouchHelper(SwipeToDeleteCallback(listMessageAdapter))
        itemTouchHelper.attachToRecyclerView(messagesView)

        swipeRefreshLayout.setOnRefreshListener { onRefresh() }
        binding.drawerLayout.addDrawerListener(
            object : SimpleDrawerListener() {
                override fun onDrawerOpened(drawerView: View) {
                    onBackPressedCallback.isEnabled = true
                }

                override fun onDrawerClosed(drawerView: View) {
                    onBackPressedCallback.isEnabled = false
                }
            }
        )

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
    }

    private fun bindViewModel() {
        lifecycleScope.launch {
            messagesViewModel.messagesWithImage.collectLatest { messages ->
                listMessageAdapter.updateList(messages)
                when (messages.isEmpty()) {
                    true -> binding.flipper.displayedChild = 1
                    false -> binding.flipper.displayedChild = 0
                }
            }
        }
        lifecycleScope.launch {
            messagesViewModel.refreshing.collectLatest { refreshing ->
                println("JcLog: refreshing $refreshing")
                swipeRefreshLayout.isRefreshing = refreshing
            }
        }
        lifecycleScope.launch {
            messagesViewModel.applicationsState.collect { applications ->
                println("JcLog: $applications")
                println("JcLog: applications (${applications.size})")
                lifecycleScope.launch(Dispatchers.Main) { onUpdateApps(applications) }
            }
        }
        lifecycleScope.launch {
            messagesViewModel.menuMode.collect {
                menuMode = it
                binding.appBarDrawer.toolbar.subtitle = when (it) {
                    is AllAppMenuMode -> ""
                    is AppMenuMode -> it.app.name
                }
                invalidateMenu()
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        binding.learnGotify.setOnClickListener { openDocumentation() }
    }

    private fun refreshAll() {
        CoilInstance.evict(this)
        messagesViewModel.onRefreshAll()
    }

    private fun onRefresh() {
        CoilInstance.evict(this)
        messagesViewModel.onRefreshMessages()
    }

    private fun openDocumentation() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://gotify.net/docs/pushmsg"))
        startActivity(browserIntent)
    }

    private fun onUpdateApps(applicationsState: List<ApplicationState>) {
        val menu: Menu = binding.navView.menu
        menu.removeGroup(R.id.apps)
        applicationsState.forEachIndexed { index, applicationState ->
            menu.add(
                R.id.apps,
                index,
                APPLICATION_ORDER,
                applicationState.application.name
            ).apply {
                setOnMenuItemClickListener {
                    messagesViewModel.onSelectApplication(applicationState.application)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                if (applicationState.unreadCount > 0) {
                    setActionView(R.layout.action_menu_counter)
                    actionView?.findViewById<TextView>(
                        R.id.counter
                    )?.text = applicationState.counterLabel
                }
                val request = ImageRequest.Builder(this@MessagesActivity)
                    .data(applicationState.iconUrl)
                    .error(R.drawable.ic_alarm)
                    .placeholder(R.drawable.ic_placeholder)
                    .size(100, 100)
                    .target(Utils.toDrawable { icon -> this.icon = icon })
                    .build()
                CoilInstance.get(this@MessagesActivity).enqueue(request)
            }
        }
    }

    private val ApplicationState.counterLabel: String
        get() = min(unreadCount, 99)
            .toString() + ("+".takeIf { hasMoreMessages || unreadCount > 99 } ?: "")

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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.nav_all_messages) {
            updateAppOnDrawerClose = MessageState.ALL_MESSAGES
            messagesViewModel.onDeselectApplication()
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

    override fun onResume() {
        val context = applicationContext
        val nManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nManager.cancelAll()
        super.onResume()
    }

    private fun shadowDelete(message: Message) {
        messagesViewModel.onShadowDeleteMessage(message)
        showDeletionSnackbar(message)
    }

    private fun showDeletionSnackbar(message: Message) {
        val view: View = binding.swipeRefresh
        val snackbar = Snackbar.make(view, R.string.snackbar_deleted, Snackbar.LENGTH_LONG)
        snackbar.setAction(R.string.snackbar_undo) {
            messagesViewModel.onUnShadowDeleteMessage(message)
        }
        snackbar.addCallback(
            SnackbarCallback(message)
        )
        snackbar.show()
    }

    private inner class SnackbarCallback(private val message: Message) : BaseCallback<Snackbar?>() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            super.onDismissed(transientBottomBar, event)
            if (event != DISMISS_EVENT_ACTION) {
                messagesViewModel.onDeleteMessage(message)
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
            shadowDelete(message.message)
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
                    totalItemCount != 0
                ) {
                    messagesViewModel.onLoadMore()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.messages_action, menu)
        menu.findItem(R.id.action_delete_app).isVisible = menuMode != AllAppMenuMode
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_delete_all) {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_all)
                .setMessage(R.string.ack)
                .setPositiveButton(R.string.yes) { _, _ ->
                    messagesViewModel.deleteAllMessages(menuMode)
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }
        (menuMode as? AppMenuMode)
            ?.let { appMenuMode ->
                if (item.itemId == R.id.action_delete_app) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete_app)
                        .setMessage(R.string.ack)
                        .setPositiveButton(R.string.yes) { _, _ ->
                            messagesViewModel.deleteApp(appMenuMode.app)
                        }
                        .setNegativeButton(R.string.no, null)
                        .show()
                }
            }
        return super.onContextItemSelected(item)
    }

    private fun deleteClientAndNavigateToLogin() {
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
        settings.clear()
        startActivity(Intent(this@MessagesActivity, LoginActivity::class.java))
        finish()
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
