package com.github.gotify.picasso

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.gotify.R
import com.github.gotify.Settings
import com.github.gotify.Utils
import com.github.gotify.api.Callback
import com.github.gotify.api.CertUtils
import com.github.gotify.api.ClientFactory
import com.github.gotify.client.api.ApplicationApi
import com.github.gotify.log.Log
import com.github.gotify.messages.provider.MessageImageCombiner
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

internal class PicassoHandler(private val context: Context, private val settings: Settings) {
    companion object {
        private const val PICASSO_CACHE_SIZE = 50 * 1024 * 1024 // 50 MB
        private const val PICASSO_CACHE_SUBFOLDER = "picasso-cache"
    }

    private val picassoCache = Cache(
        File(context.cacheDir, PICASSO_CACHE_SUBFOLDER),
        PICASSO_CACHE_SIZE.toLong()
    )

    private val picasso = makePicasso()
    private val appIdToAppImage = ConcurrentHashMap<Long, String>()

    private fun makePicasso(): Picasso {
        val builder = OkHttpClient.Builder()
        builder.cache(picassoCache)
        CertUtils.applySslSettings(builder, settings.sslSettings())
        val downloader = OkHttp3Downloader(builder.build())
        return Picasso.Builder(context)
            .addRequestHandler(PicassoDataRequestHandler())
            .downloader(downloader)
            .build()
    }

    @Throws(IOException::class)
    fun getImageFromUrl(url: String?): Bitmap = picasso.load(url).get()

    fun getIcon(appId: Long): Bitmap {
        if (appId == -1L) {
            return BitmapFactory.decodeResource(context.resources, R.drawable.gotify)
        }
        try {
            return getImageFromUrl(
                Utils.resolveAbsoluteUrl("${settings.url}/", appIdToAppImage[appId])
            )
        } catch (e: IOException) {
            Log.e("Could not load image for notification", e)
        }
        return BitmapFactory.decodeResource(context.resources, R.drawable.gotify)
    }

    fun updateAppIds() {
        ClientFactory.clientToken(settings.url, settings.sslSettings(), settings.token)
            .createService(ApplicationApi::class.java)
            .apps
            .enqueue(
                Callback.call(
                    onSuccess = { apps ->
                        appIdToAppImage.clear()
                        appIdToAppImage.putAll(MessageImageCombiner.appIdToImage(apps))
                    },
                    onError = { appIdToAppImage.clear() }
                )
            )
    }

    fun get() = picasso

    @Throws(IOException::class)
    fun evict() {
        picassoCache.evictAll()
    }
}
