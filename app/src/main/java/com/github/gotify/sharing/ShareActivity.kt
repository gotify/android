package com.github.gotify.sharing

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.gotify.R
import com.github.gotify.Settings
import com.github.gotify.Utils
import com.github.gotify.api.Api
import com.github.gotify.api.ApiException
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.MessageApi
import com.github.gotify.client.model.Application
import com.github.gotify.client.model.Message
import com.github.gotify.databinding.ActivityShareBinding
import com.github.gotify.log.Log
import com.github.gotify.messages.provider.ApplicationHolder

class ShareActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShareBinding
    private lateinit var settings: Settings
    private lateinit var appsHolder: ApplicationHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.i("Entering ${javaClass.simpleName}")
        setSupportActionBar(binding.appBarDrawer.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowCustomEnabled(true)
        }
        settings = Settings(this)

        val intent = intent
        val type = intent.type
        if (Intent.ACTION_SEND == intent.action && "text/plain" == type) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                binding.content.setText(sharedText)
            }
        }

        if (!settings.tokenExists()) {
            Toast.makeText(applicationContext, R.string.not_loggedin_share, Toast.LENGTH_SHORT)
                .show()
            finish()
            return
        }

        val client = ClientFactory.clientToken(
            settings.url(),
            settings.sslSettings(),
            settings.token()
        )
        appsHolder = ApplicationHolder(this, client)
        appsHolder.onUpdate {
            val apps = appsHolder.get()
            populateSpinner(apps)

            val appsAvailable = apps.isNotEmpty()
            binding.pushButton.isEnabled = appsAvailable
            binding.missingAppsContainer.visibility = if (appsAvailable) View.GONE else View.VISIBLE
        }
        appsHolder.onUpdateFailed { binding.pushButton.isEnabled = false }
        appsHolder.request()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        binding.pushButton.setOnClickListener { pushMessage() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.home) {
            finish()
            return true
        }
        return false
    }

    fun pushMessage() {
        val titleText = binding.title.text.toString()
        val contentText = binding.content.text.toString()
        val priority = binding.edtTxtPriority.text.toString()
        val appIndex = binding.appSpinner.selectedItemPosition

        if (contentText.isEmpty()) {
            Toast.makeText(this, "Content should not be empty.", Toast.LENGTH_LONG).show()
            return
        } else if (priority.isEmpty()) {
            Toast.makeText(this, "Priority should be number.", Toast.LENGTH_LONG).show()
            return
        } else if (appIndex == Spinner.INVALID_POSITION) {
            // For safety, e.g. loading the apps needs too much time (maybe a timeout) and
            // the user tries to push without an app selected.
            Toast.makeText(this, "An app must be selected.", Toast.LENGTH_LONG).show()
            return
        }

        val message = Message()
        if (titleText.isNotEmpty()) {
            message.title = titleText
        }
        message.message = contentText
        message.priority = priority.toLong()

        PushMessage({
            val pushClient = ClientFactory.clientToken(
                settings.url(),
                settings.sslSettings(),
                appsHolder.get()[appIndex].token
            )
            try {
                val messageApi = pushClient.createService(MessageApi::class.java)
                Api.execute(messageApi.createMessage(it))
                "Pushed!"
            } catch (apiException: ApiException) {
                Log.e("Failed sending message", apiException)
                "Oops! Something went wrong..."
            }
        }, {
            Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            finish()
        }).execute(message)
    }

    private fun populateSpinner(apps: List<Application>) {
        val appNameList = mutableListOf<String>()
        apps.forEach { app ->
            appNameList.add(app.name)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, appNameList)
        binding.appSpinner.adapter = adapter
    }

    class PushMessage(
        private val backgroundAction: (message: Message?) -> String,
        private val postAction: (message: String) -> Unit
    ) : AsyncTask<Message?, String?, String>() {
        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg messages: Message?): String {
            return backgroundAction(Utils.first(messages))
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(message: String) {
            postAction(message)
        }
    }
}
