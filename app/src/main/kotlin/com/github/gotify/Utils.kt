package com.github.gotify

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.target.Target
import com.github.gotify.client.JSON
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.threeten.bp.OffsetDateTime
import org.tinylog.kotlin.Logger

internal object Utils {
    val JSON: Gson = JSON().gson

    fun showSnackBar(activity: Activity, message: String?) {
        val rootView = activity.window.decorView.findViewById<View>(android.R.id.content)
        Snackbar.make(rootView, message!!, Snackbar.LENGTH_SHORT).show()
    }

    fun longToInt(value: Long): Int {
        return (value % Int.MAX_VALUE).toInt()
    }

    fun dateToRelative(data: OffsetDateTime): String {
        val time = data.toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS)
            .toString()
    }

    fun resolveAbsoluteUrl(baseURL: String, target: String?): String? {
        return if (target == null) {
            null
        } else {
            try {
                val targetUri = URI(target)
                if (targetUri.isAbsolute) {
                    target
                } else {
                    URL(URL(baseURL), target).toString()
                }
            } catch (e: MalformedURLException) {
                Logger.error(e, "Could not resolve absolute url")
                target
            } catch (e: URISyntaxException) {
                Logger.error(e, "Could not resolve absolute url")
                target
            }
        }
    }

    fun toDrawable(drawableReceiver: DrawableReceiver): Target {
        return object : Target {
            override fun onSuccess(result: Drawable) {
                drawableReceiver.loaded(result)
            }

            override fun onError(error: Drawable?) {
                Logger.error("Bitmap failed")
            }
        }
    }

    fun AppCompatActivity.launchCoroutine(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        action: suspend (coroutineScope: CoroutineScope) -> Unit
    ) {
        this.lifecycleScope.launch(dispatcher) {
            action(this)
        }
    }

    fun interface DrawableReceiver {
        fun loaded(drawable: Drawable?)
    }

    fun setExcludeFromRecent(context: Context, excludeFromRecent: Boolean) {
        context.getSystemService(ActivityManager::class.java).appTasks?.getOrNull(0)
            ?.setExcludeFromRecents(excludeFromRecent)
    }

    fun redactPassword(stringUrl: String?): String {
        val url = stringUrl?.toHttpUrlOrNull()
        return when {
            url == null -> "unknown"
            url.password.isEmpty() -> url.toString()
            else -> url.newBuilder().password("REDACTED").toString()
        }
    }
}
