package com.github.gotify

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.executeBlocking
import coil.request.ImageRequest
import com.github.gotify.api.CertUtils
import com.github.gotify.client.model.Application
import java.io.IOException
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.tinylog.kotlin.Logger

object CoilInstance {
    private var holder: Pair<SSLSettings, ImageLoader>? = null

    @Throws(IOException::class)
    fun getImageFromUrl(context: Context, url: String?): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()
        return (get(context).executeBlocking(request).drawable as BitmapDrawable).bitmap
    }

    fun getIcon(context: Context, app: Application?): Bitmap {
        if (app == null) {
            return BitmapFactory.decodeResource(context.resources, R.drawable.gotify)
        }
        val baseUrl = Settings(context).url
        try {
            return getImageFromUrl(
                context,
                Utils.resolveAbsoluteUrl("$baseUrl/", app.image)
            )
        } catch (e: IOException) {
            Logger.error(e, "Could not load image for notification")
        }
        return BitmapFactory.decodeResource(context.resources, R.drawable.gotify)
    }

    @OptIn(ExperimentalCoilApi::class)
    fun evict(context: Context) {
        try {
            get(context).apply {
                diskCache?.clear()
                memoryCache?.clear()
            }
        } catch (e: IOException) {
            Logger.error(e, "Problem evicting Coil cache")
        }
    }

    @Synchronized
    fun get(context: Context): ImageLoader {
        val newSettings = Settings(context).sslSettings()
        val copy = holder
        if (copy != null && copy.first == newSettings) {
            return copy.second
        }
        return makeImageLoader(context, newSettings).also { holder = it }.second
    }

    private fun makeImageLoader(
        context: Context,
        sslSettings: SSLSettings
    ): Pair<SSLSettings, ImageLoader> {
        val builder = OkHttpClient
            .Builder()
            .addInterceptor(BasicAuthInterceptor())
        CertUtils.applySslSettings(builder, sslSettings)
        val loader = ImageLoader.Builder(context)
            .okHttpClient(builder.build())
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil-cache"))
                    .build()
            }
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
        return sslSettings to loader
    }
}

private class BasicAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // If there's no username, skip the authentication
        if (request.url.username.isNotEmpty()) {
            val basicAuthString = "${request.url.username}:${request.url.password}@"
            val url = request.url.toString().replace(basicAuthString, "")
            request = request
                .newBuilder()
                .header(
                    "Authorization",
                    Credentials.basic(request.url.username, request.url.password)
                )
                .url(url)
                .build()
        }
        return chain.proceed(request)
    }
}
