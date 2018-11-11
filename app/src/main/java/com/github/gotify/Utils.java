package com.github.gotify;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.View;
import androidx.annotation.NonNull;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.JSON;
import com.github.gotify.log.Log;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import okio.Buffer;
import org.threeten.bp.OffsetDateTime;

public class Utils {
    public static void showSnackBar(Activity activity, String message) {
        View rootView = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    public static String dateToRelative(OffsetDateTime data) {
        long time = data.toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS)
                .toString();
    }

    public static Target toDrawable(Resources resources, DrawableReceiver drawableReceiver) {
        return new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                drawableReceiver.loaded(new BitmapDrawable(resources, bitmap));
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                Log.e("Bitmap failed", e);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        };
    }

    public static JSON json() {
        return new ApiClient().getJSON();
    }

    public static String readFileFromStream(@NonNull InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        String currentLine;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((currentLine = reader.readLine()) != null) {
                sb.append(currentLine).append("\n");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to read input");
        }

        return sb.toString();
    }

    public interface DrawableReceiver {
        void loaded(Drawable drawable);
    }

    public static InputStream stringToInputStream(String str) {
        if (str == null) return null;
        return new Buffer().writeUtf8(str).inputStream();
    }
}
