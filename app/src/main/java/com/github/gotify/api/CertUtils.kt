package com.github.gotify.api

import android.annotation.SuppressLint
import com.github.gotify.SSLSettings
import com.github.gotify.Utils
import com.github.gotify.log.Log
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*
import okhttp3.OkHttpClient

internal object CertUtils {
    @SuppressLint("CustomX509TrustManager")
    private val trustAll = object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}

        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
    }

    fun parseCertificate(cert: String): Certificate {
        try {
            val certificateFactory = CertificateFactory.getInstance("X509")
            return certificateFactory.generateCertificate(Utils.stringToInputStream(cert))
        } catch (e: Exception) {
            throw IllegalArgumentException("certificate is invalid")
        }
    }

    fun applySslSettings(builder: OkHttpClient.Builder, settings: SSLSettings) {
        // Modified from ApiClient.applySslSettings in the client package.
        try {
            if (!settings.validateSSL) {
                val context = SSLContext.getInstance("TLS")
                context.init(arrayOf(), arrayOf<TrustManager>(trustAll), SecureRandom())
                builder.sslSocketFactory(context.socketFactory, trustAll)
                builder.hostnameVerifier { _, _ -> true }
                return
            }
            val cert = settings.cert
            if (cert != null) {
                val trustManagers = certToTrustManager(cert)
                if (trustManagers.isNotEmpty()) {
                    val context = SSLContext.getInstance("TLS")
                    context.init(arrayOf(), trustManagers, SecureRandom())
                    builder.sslSocketFactory(
                        context.socketFactory,
                        trustManagers[0] as X509TrustManager
                    )
                }
            }
        } catch (e: Exception) {
            // We shouldn't have issues since the cert is verified on login.
            Log.e("Failed to apply SSL settings", e)
        }
    }

    @Throws(GeneralSecurityException::class)
    private fun certToTrustManager(cert: String): Array<TrustManager> {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificates = certificateFactory.generateCertificates(Utils.stringToInputStream(cert))
        require(certificates.isNotEmpty()) { "expected non-empty set of trusted certificates" }

        val caKeyStore = newEmptyKeyStore()
        certificates.forEachIndexed { index, certificate ->
            val certificateAlias = "ca$index"
            caKeyStore.setCertificateEntry(certificateAlias, certificate)
        }
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(caKeyStore)
        return trustManagerFactory.trustManagers
    }

    @Throws(GeneralSecurityException::class)
    private fun newEmptyKeyStore(): KeyStore {
        return try {
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore
        } catch (e: IOException) {
            throw AssertionError(e)
        }
    }
}
