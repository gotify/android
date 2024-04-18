package com.github.gotify

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.gotify.client.JSON
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    fun toDrawable(resources: Resources?, drawableReceiver: DrawableReceiver): Target {
        return object : Target {
            override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
                drawableReceiver.loaded(BitmapDrawable(resources, bitmap))
            }

            override fun onBitmapFailed(e: Exception, errorDrawable: Drawable) {
                Logger.error(e, "Bitmap failed")
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable) {}
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
}
