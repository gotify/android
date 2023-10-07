package com.github.gotify.picasso

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.gotify.R
import com.github.gotify.Settings
import com.github.gotify.Utils
import com.github.gotify.api.CertUtils
import com.github.gotify.client.model.Application
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import java.io.File
import java.io.IOException
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.tinylog.kotlin.Logger

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

    fun getIcon(app: Application?): Bitmap {
        if (app == null) {
            return BitmapFactory.decodeResource(context.resources, R.drawable.gotify)
        }
        try {
            return getImageFromUrl(
                Utils.resolveAbsoluteUrl("${settings.url}/", app.image)
            )
        } catch (e: IOException) {
            Logger.error(e, "Could not load image for notification")
        }
        return BitmapFactory.decodeResource(context.resources, R.drawable.gotify)
    }

    fun get() = picasso

    @Throws(IOException::class)
    fun evict() {
        picassoCache.evictAll()
    }
}
