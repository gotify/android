package com.github.alertify.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.github.alertify.R;
import com.github.alertify.Settings;
import com.github.alertify.Utils;
import com.github.alertify.api.Callback;
import com.github.alertify.api.CertUtils;
import com.github.alertify.api.ClientFactory;
import com.github.alertify.client.api.ApplicationApi;
import com.github.alertify.log.Log;
import com.github.alertify.init.messages.provider.MessageImageCombiner;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class PicassoHandler {

    private static final int PICASSO_CACHE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final String PICASSO_CACHE_SUBFOLDER = "picasso-cache";

    private Context context;
    private Settings settings;

    private Cache picassoCache;

    private Picasso picasso;
    private Map<Long, String> appIdToAppImage = new ConcurrentHashMap<>();

    public PicassoHandler(Context context, Settings settings) {
        this.context = context;
        this.settings = settings;

        picassoCache =
                new Cache(
                        new File(context.getCacheDir(), PICASSO_CACHE_SUBFOLDER),
                        PICASSO_CACHE_SIZE);
        picasso = makePicasso();
    }

    private Picasso makePicasso() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cache(picassoCache);
        CertUtils.applySslSettings(builder, settings.sslSettings());
        OkHttp3Downloader downloader = new OkHttp3Downloader(builder.build());
        return new Picasso.Builder(context).downloader(downloader).build();
    }

    public Bitmap getIcon(Long appId) {
        if (appId == -1) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.alertify);
        }

        try {
            return picasso.load(
                            Utils.resolveAbsoluteUrl(
                                    settings.url() + "/", appIdToAppImage.get(appId)))
                    .get();
        } catch (IOException e) {
            Log.e("Could not load image for notification", e);
        }
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.alertify);
    }

    public void updateAppIds() {
        ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token())
                .createService(ApplicationApi.class)
                .getApps()
                .enqueue(
                        Callback.call(
                                (apps) -> {
                                    appIdToAppImage.clear();
                                    appIdToAppImage.putAll(MessageImageCombiner.appIdToImage(apps));
                                },
                                (t) -> {
                                    appIdToAppImage.clear();
                                }));
    }

    public Picasso get() {
        return picasso;
    }

    public void evict() throws IOException {
        picassoCache.evictAll();
    }
}
