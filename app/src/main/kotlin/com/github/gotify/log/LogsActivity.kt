package com.github.gotify.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.github.gotify.R
import com.github.gotify.Utils
import com.github.gotify.Utils.launchCoroutine
import com.github.gotify.databinding.ActivityLogsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tinylog.kotlin.Logger

internal class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Logger.info("Entering ${javaClass.simpleName}")
        updateLogs()
        setSupportActionBar(binding.appBarDrawer.toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setDisplayShowCustomEnabled(true)
        }
    }

    private fun updateLogs() {
        launchCoroutine {
            val log = LoggerHelper.read(this)
            withContext(Dispatchers.Main) {
                val content = binding.logContent
                if (content.selectionStart == content.selectionEnd) {
                    content.text = log
                }
            }
        }

        if (!isDestroyed) {
            handler.postDelayed({ updateLogs() }, 5000)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.logs_action, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            R.id.action_delete_logs -> {
                LoggerHelper.clear(this)
                binding.logContent.text = null
                true
            }

            R.id.action_copy_logs -> {
                val content = binding.logContent
                val clipboardManager =
                    getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("GotifyLog", content.text.toString())
                clipboardManager.setPrimaryClip(clipData)
                Utils.showSnackBar(this, getString(R.string.logs_copied))
                true
            }

            else -> false
        }
    }
}
