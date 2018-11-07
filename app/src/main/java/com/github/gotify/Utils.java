package com.github.gotify;

import android.annotation.SuppressLint;
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
import okhttp3.OkHttpClient;
import okio.Buffer;
import org.threeten.bp.OffsetDateTime;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.InputStream;
import java.util.Collection;

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

    public interface DrawableReceiver {
        void loaded(Drawable drawable);
    }

    public static InputStream stringToInputStream(String str) {
        if (str == null) return null;
        return new Buffer()
                .writeUtf8(str)
                .inputStream();
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

    /////////////////////////
    ///// SSL Utilities /////
    /////////////////////////
    public static Certificate parseCertificate(String cert) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");

            return certificateFactory.generateCertificate(Utils.stringToInputStream(cert));
        } catch (Exception e) {
            throw new IllegalArgumentException("certificate is invalid");
        }
    }

    public static class SSLSettings {
        boolean validateSSL;
        String cert;

        public SSLSettings(boolean validateSSL, String cert) {
            this.validateSSL = validateSSL;
            this.cert = cert;
        }
    }

    // TrustManager that trusts all SSL Certs
    private static final TrustManager trustAll = new X509TrustManager() {
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        @SuppressLint("TrustAllX509TrustManager")
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
        @Override
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
    };

    public static void applySslSettings(OkHttpClient.Builder builder, SSLSettings settings) {
        // Modified from ApiClient.applySslSettings in the client package.

        try {
            TrustManager[] trustManagers = null;
            HostnameVerifier hostnameVerifier = null;
            if (!settings.validateSSL) {
                trustManagers = new TrustManager[]{ trustAll };
                hostnameVerifier = (hostname, session) -> true;
            } else if (settings.cert != null) {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(stringToInputStream(settings.cert));
                if (certificates.isEmpty()) {
                    throw new IllegalArgumentException("expected non-empty set of trusted certificates");
                }
                KeyStore caKeyStore = newEmptyKeyStore();
                int index = 0;
                for (Certificate certificate : certificates) {
                    String certificateAlias = "ca" + Integer.toString(index++);
                    caKeyStore.setCertificateEntry(certificateAlias, certificate);
                }
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(caKeyStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            }

            if (trustManagers != null && trustManagers.length > 0) {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(new KeyManager[]{}, trustManagers, new SecureRandom());
                builder.sslSocketFactory(context.getSocketFactory(), (X509TrustManager) trustManagers[0]);
            }

            if (hostnameVerifier != null) builder.hostnameVerifier(hostnameVerifier);
        } catch (Exception e) {
            // We shouldn't have issues since the cert is verified on login.
            Log.e("Failed to apply SSL settings", e);
        }
    }

    private static KeyStore newEmptyKeyStore() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
