package com.github.gotify.api;

import android.annotation.SuppressLint;
import android.os.Build;
import androidx.annotation.RequiresApi;
import com.github.gotify.SSLSettings;
import com.github.gotify.Utils;
import com.github.gotify.log.Log;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;

public class CertUtils {
    private static final X509TrustManager trustAll =
            new X509TrustManager() {
                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @SuppressLint("TrustAllX509TrustManager")
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[] {};
                }
            };

    public static Certificate parseCertificate(String cert) {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");

            return certificateFactory.generateCertificate(Utils.stringToInputStream(cert));
        } catch (Exception e) {
            throw new IllegalArgumentException("certificate is invalid");
        }
    }

    public static void applySslSettings(OkHttpClient.Builder builder, SSLSettings settings) {
        // Modified from ApiClient.applySslSettings in the client package.

        try {
            if (!settings.validateSSL) {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(
                        new KeyManager[] {}, new TrustManager[] {trustAll}, new SecureRandom());
                builder.sslSocketFactory(context.getSocketFactory(), trustAll);
                builder.hostnameVerifier((a, b) -> true);
                return;
            }

            if (settings.cert != null) {
                KeyManager[] keyManagers = new KeyManager[] {};
                TrustManager[] trustManagers = certToTrustManager(settings.cert);

                if (trustManagers != null && trustManagers.length > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (settings.clientCert != null) {
                            keyManagers = getClientCerts(settings.clientCert, settings.clientCertPassword.toCharArray());
                        }
                    }

                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(keyManagers, trustManagers, new SecureRandom());
                    builder.sslSocketFactory(
                            context.getSocketFactory(), (X509TrustManager) trustManagers[0]);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (settings.clientCert != null) {
                        KeyManager[] keyManagers = getClientCerts(settings.clientCert, settings.clientCertPassword.toCharArray());

                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                                TrustManagerFactory.getDefaultAlgorithm());
                        trustManagerFactory.init((KeyStore) null);
                        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                            throw new IllegalStateException("Unexpected default trust managers:");
                        }
                        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                        SSLContext context = SSLContext.getInstance("TLS");
                        context.init(keyManagers, new TrustManager[] { trustManager }, null);
                        builder.sslSocketFactory(
                                context.getSocketFactory(), trustManager);
                    }
                }
            }
        }
        catch (Exception e) {
            // We shouldn't have issues since the cert is verified on login.
            Log.e("Failed to apply SSL settings", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static KeyManager[] getClientCerts(String clientCert, char[] password)
            throws CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        InputStream bs = new ByteArrayInputStream(Base64.getDecoder().decode(clientCert));
        ks.load(bs, password);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("X509");
        kmf.init(ks, password);

        return kmf.getKeyManagers();
    }

    private static TrustManager[] certToTrustManager(String cert) throws GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates =
                certificateFactory.generateCertificates(Utils.stringToInputStream(cert));
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }
        KeyStore caKeyStore = newEmptyKeyStore();
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = "ca" + Integer.toString(index++);
            caKeyStore.setCertificateEntry(certificateAlias, certificate);
        }
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);
        return trustManagerFactory.getTrustManagers();
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
