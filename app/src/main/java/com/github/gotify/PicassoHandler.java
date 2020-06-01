package com.github.gotify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.github.gotify.api.CertUtils;
import com.github.gotify.api.ClientFactory;
import com.github.gotify.client.ApiClient;
import com.github.gotify.client.api.ApplicationApi;
import com.github.gotify.client.model.Application;
import com.github.gotify.messages.MessagesActivity;
import com.github.gotify.messages.provider.MessageImageCombiner;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

/**
 * Copyright (C) 2019  Felix Nüsse
 * Created on 01.06.20 - 20:03
 * <p>
 * Edited by: Felix Nüsse felix.nuesse(at)t-online.de
 * <p>
 * gotify-android
 * <p>
 * This program is released under the GPLv3 license
 * <p>
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
public class PicassoHandler {

    public static int PICASSO_CACHE_SIZE = 50 * 1024 * 1024; // 50 MB
    public static String PICASSO_CACHE_SUBFOLDER = "picasso-cache";

    private Context context;
    private Settings settings;

    private Cache picassoCache;

    private Picasso picasso;
    private Map<Integer, String> appIdToAppImage = new ConcurrentHashMap<>();

    public PicassoHandler(Context context, Settings settings) {
        this.context = context;
        this.settings = settings;

        picasso = makePicasso();

    }

    private void prepareCache(){
        if(picassoCache == null){
            picassoCache = new Cache(new File(context.getCacheDir(), PICASSO_CACHE_SUBFOLDER), PICASSO_CACHE_SIZE);
        }
    }

    private Picasso makePicasso() {
        prepareCache();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.cache(picassoCache);
        CertUtils.applySslSettings(builder, settings.sslSettings());
        OkHttp3Downloader downloader = new OkHttp3Downloader(builder.build());
        return new Picasso.Builder(context).downloader(downloader).build();
    }


    public Bitmap getIcon(Integer appId) {
        if(appId==-1){
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.gotify);
        }

        try {
            return picasso.load(Utils.resolveAbsoluteUrl(settings.url() + "/", appIdToAppImage.get(appId))).get();
        } catch (IOException e) {
            com.github.gotify.log.Log.e("Could not load image for notification", e);
        }
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.gotify);
    }


    public void updateAppIds(){
        Thread thread = new Thread(() -> {
            try  {
                updateAppIds_nonasync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private void updateAppIds_nonasync(){
        ApiClient client = ClientFactory.clientToken(settings.url(), settings.sslSettings(), settings.token());

        List<Application> applications = null;
        try {
            applications = client.createService(ApplicationApi.class).getApps().execute().body();
            appIdToAppImage = MessageImageCombiner.appIdToImage(applications);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Picasso exposedPicasso() {
        return picasso;
    }

    public void evict() throws IOException {
        picassoCache.evictAll();
    }
}
