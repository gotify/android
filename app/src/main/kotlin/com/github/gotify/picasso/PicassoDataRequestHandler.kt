package com.github.gotify.picasso

import android.graphics.BitmapFactory
import android.util.Base64
import com.github.gotify.log.Log
import com.squareup.picasso.Picasso
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler

/**
 * Adapted from https://github.com/square/picasso/issues/1395#issuecomment-220929377 By
 * https://github.com/SmartDengg
 */
internal class PicassoDataRequestHandler : RequestHandler() {
    companion object {
        private const val DATA_SCHEME = "data"
    }

    override fun canHandleRequest(data: Request): Boolean {
        val scheme = data.uri.scheme
        return DATA_SCHEME.equals(scheme, ignoreCase = true)
    }

    override fun load(request: Request, networkPolicy: Int): Result {
        val uri = request.uri.toString()
        val imageDataBytes = uri.substring(uri.indexOf(",") + 1)
        val bytes = Base64.decode(imageDataBytes.toByteArray(), Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        if (bitmap == null) {
            val show = if (uri.length > 50) uri.take(50) + "..." else uri
            val malformed = RuntimeException("Malformed data uri: $show")
            Log.e("Could not load image", malformed)
            throw malformed
        }

        return Result(bitmap, Picasso.LoadedFrom.NETWORK)
    }
}
