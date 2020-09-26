package com.github.alertify.picasso;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import com.github.alertify.log.Log;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;

/**
 * Adapted from https://github.com/square/picasso/issues/1395#issuecomment-220929377 By
 * https://github.com/SmartDengg
 */
public class PicassoDataRequestHandler extends RequestHandler {

    private static final String DATA_SCHEME = "data";

    @Override
    public boolean canHandleRequest(Request data) {
        String scheme = data.uri.getScheme();
        return DATA_SCHEME.equalsIgnoreCase(scheme);
    }

    @Override
    public Result load(Request request, int networkPolicy) {
        String uri = request.uri.toString();
        String imageDataBytes = uri.substring(uri.indexOf(",") + 1);
        byte[] bytes = Base64.decode(imageDataBytes.getBytes(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        if (bitmap == null) {
            String show = uri.length() > 50 ? uri.substring(0, 49) + "..." : uri;
            RuntimeException malformed = new RuntimeException("Malformed data uri: " + show);
            Log.e("Could not load image", malformed);
            throw malformed;
        }

        return new Result(bitmap, Picasso.LoadedFrom.NETWORK);
    }
}
