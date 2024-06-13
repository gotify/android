package com.github.gotify.sharing

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.gotify.R
import com.github.gotify.Settings
import com.github.gotify.Utils.launchCoroutine
import com.github.gotify.api.Api
import com.github.gotify.api.ApiException
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.MessageApi
import com.github.gotify.client.model.Application
import com.github.gotify.client.model.Message
import com.github.gotify.databinding.ActivityShareBinding
import com.github.gotify.messages.provider.ApplicationHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tinylog.kotlin.Logger

internal class ShareActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShareBinding
    private lateinit var settings: Settings
    private lateinit var appsHolder: ApplicationHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Logger.info("Entering ${javaClass.simpleName}")
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
            Toast.makeText(
                applicationContext,
                R.string.not_loggedin_share,
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        val client = ClientFactory.clientToken(settings)
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
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return false
    }

    private fun pushMessage() {
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

        launchCoroutine {
            val response = executeMessageCall(appIndex, message)
            withContext(Dispatchers.Main) {
                if (response) {
                    Toast.makeText(this@ShareActivity, "Pushed!", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(
                        this@ShareActivity,
                        "Oops! Something went wrong...",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun executeMessageCall(appIndex: Int, message: Message): Boolean {
        val pushClient = ClientFactory.clientToken(settings, appsHolder.get()[appIndex].token)
        return try {
            val messageApi = pushClient.createService(MessageApi::class.java)
            Api.execute(messageApi.createMessage(message))
            true
        } catch (apiException: ApiException) {
            Logger.error(apiException, "Failed sending message")
            false
        }
    }

    private fun populateSpinner(apps: List<Application>) {
        val appNameList = mutableListOf<String>()
        apps.forEach {
            appNameList.add(it.name)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, appNameList)
        binding.appSpinner.adapter = adapter
    }
}
