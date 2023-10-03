package com.github.gotify.messages

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.gotify.databinding.ActivityDialogIntentUrlBinding

internal class IntentUrlDialogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        val binding = ActivityDialogIntentUrlBinding.inflate(layoutInflater)
        val intentUrl = intent.getStringExtra(EXTRA_KEY_URL)
        assert(intentUrl != null) { "intentUrl may not be empty" }

        binding.urlView.text = intentUrl
        binding.openButton.setOnClickListener {
            finish()
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(intentUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(this)
            }
        }
        binding.cancelButton.setOnClickListener { finish() }
        setContentView(binding.root)
    }

    companion object {
        const val EXTRA_KEY_URL = "url"
    }
}
