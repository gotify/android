package com.github.gotify.api

import android.annotation.SuppressLint
import com.github.gotify.SSLSettings
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import okhttp3.OkHttpClient
import org.tinylog.kotlin.Logger

internal object CertUtils {
    const val CA_CERT_NAME = "ca-cert.crt"
    const val CLIENT_CERT_NAME = "client-cert.p12"

    @SuppressLint("CustomX509TrustManager")
    private val trustAll = object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        }

        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
    }

    fun parseCertificate(inputStream: InputStream): Certificate {
        try {
            val certificateFactory = CertificateFactory.getInstance("X509")
            return certificateFactory.generateCertificate(inputStream)
        } catch (e: Exception) {
            throw IllegalArgumentException("certificate is invalid")
        }
    }

    fun applySslSettings(builder: OkHttpClient.Builder, settings: SSLSettings) {
        // Modified from ApiClient.applySslSettings in the client package.
        try {
            val trustManagers = mutableSetOf<TrustManager>()
            val keyManagers = mutableSetOf<KeyManager>()
            if (settings.validateSSL) {
                // Custom SSL validation
                settings.caCertPath?.let { trustManagers.addAll(certToTrustManager(it)) }
            } else {
                // Disable SSL validation
                trustManagers.add(trustAll)
                builder.hostnameVerifier { _, _ -> true }
            }
            settings.clientCertPath?.let {
                keyManagers.addAll(certToKeyManager(it, settings.clientCertPassword))
            }
            if (trustManagers.isNotEmpty() || keyManagers.isNotEmpty()) {
                if (trustManagers.isEmpty()) {
                    // Fall back to system trust managers
                    trustManagers.addAll(defaultSystemTrustManager())
                }
                val context = SSLContext.getInstance("TLS")
                context.init(
                    keyManagers.toTypedArray(),
                    trustManagers.toTypedArray(),
                    SecureRandom()
                )
                builder.sslSocketFactory(
                    context.socketFactory,
                    trustManagers.elementAt(0) as X509TrustManager
                )
            }
        } catch (e: Exception) {
            // We shouldn't have issues since the cert is verified on login.
            Logger.error(e, "Failed to apply SSL settings")
        }
    }

    @Throws(GeneralSecurityException::class)
    private fun certToTrustManager(certPath: String): Array<TrustManager> {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificates = FileInputStream(File(certPath)).use(
            certificateFactory::generateCertificates
        )
        require(certificates.isNotEmpty()) { "expected non-empty set of trusted certificates" }

        val caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null) }
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
    private fun certToKeyManager(certPath: String, certPassword: String?): Array<KeyManager> {
        require(certPassword != null) { "empty client certificate password" }

        val keyStore = KeyStore.getInstance("PKCS12")
        FileInputStream(File(certPath)).use {
            keyStore.load(it, certPassword.toCharArray())
        }
        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, certPassword.toCharArray())
        return keyManagerFactory.keyManagers
    }

    private fun defaultSystemTrustManager(): Array<TrustManager> {
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(null as KeyStore?)
        return trustManagerFactory.trustManagers
    }
}
