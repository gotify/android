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
import androidx.preference.PreferenceManager
import com.github.gotify.client.JSON
import com.github.gotify.log.Log
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.Buffer
import org.threeten.bp.OffsetDateTime
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

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
                Log.e("Could not resolve absolute url", e)
                target
            } catch (e: URISyntaxException) {
                Log.e("Could not resolve absolute url", e)
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
                Log.e("Bitmap failed", e)
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable) {}
        }
    }

    fun readFileFromStream(inputStream: InputStream): String {
        val sb = StringBuilder()
        var currentLine: String?
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                while (reader.readLine().also { currentLine = it } != null) {
                    sb.append(currentLine).append("\n")
                }
            }
        } catch (e: IOException) {
            throw IllegalArgumentException("failed to read input")
        }
        return sb.toString()
    }

    fun stringToInputStream(str: String?): InputStream? {
        return if (str == null) null else Buffer().writeUtf8(str).inputStream()
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

    //call this method before super.onCreate()
    fun setTheme(activity: Activity) {
        if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean(
                activity.resources.getString(R.string.setting_key_translucent_status_bar), false
            )
        ) {
            activity.setTheme(R.style.AppTheme_NoActionBar_TranslucentStatus)
        }
    }
}
