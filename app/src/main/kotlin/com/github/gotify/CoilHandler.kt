package com.github.gotify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import coil.executeBlocking
import coil.request.ImageRequest
import com.github.gotify.api.CertUtils
import com.github.gotify.client.model.Application
import java.io.IOException
import okhttp3.OkHttpClient
import org.tinylog.kotlin.Logger

internal class CoilHandler(private val context: Context, private val settings: Settings) {
    private val imageLoader = makeImageLoader()

    private fun makeImageLoader(): ImageLoader {
        val builder = OkHttpClient.Builder()
        CertUtils.applySslSettings(builder, settings.sslSettings())
        return ImageLoader.Builder(context)
            .okHttpClient(builder.build())
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil-cache"))
                    .build()
            }
            .build()
    }

    @Throws(IOException::class)
    fun getImageFromUrl(url: String?): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()
        return (imageLoader.executeBlocking(request).drawable as BitmapDrawable).bitmap
    }

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

    fun get() = imageLoader

    @OptIn(ExperimentalCoilApi::class)
    @Throws(IOException::class)
    fun evict() {
        imageLoader.diskCache?.directory?.toFile()?.deleteRecursively()
    }
}
