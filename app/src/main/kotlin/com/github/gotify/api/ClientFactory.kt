package com.github.gotify.api

import com.github.gotify.SSLSettings
import com.github.gotify.Settings
import com.github.gotify.client.ApiClient
import com.github.gotify.client.api.UserApi
import com.github.gotify.client.api.VersionApi
import com.github.gotify.client.auth.ApiKeyAuth
import com.github.gotify.client.auth.HttpBasicAuth
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import org.tinylog.kotlin.Logger

internal object ClientFactory {
    private fun unauthorized(
        settings: Settings,
        sslSettings: SSLSettings,
        baseUrl: String
    ): ApiClient {
        return defaultClient(arrayOf(), settings, sslSettings, baseUrl)
    }

    fun basicAuth(
        settings: Settings,
        sslSettings: SSLSettings,
        username: String,
        password: String
    ): ApiClient {
        val client = defaultClient(arrayOf("basicAuth"), settings, sslSettings)
        val auth = client.apiAuthorizations["basicAuth"] as HttpBasicAuth
        auth.username = username
        auth.password = password
        return client
    }

    fun clientToken(settings: Settings, token: String? = settings.token): ApiClient {
        val client = defaultClient(arrayOf("clientTokenHeader"), settings)
        val tokenAuth = client.apiAuthorizations["clientTokenHeader"] as ApiKeyAuth
        tokenAuth.apiKey = token
        return client
    }

    fun versionApi(
        settings: Settings,
        sslSettings: SSLSettings = settings.sslSettings(),
        baseUrl: String = settings.url
    ): VersionApi {
        return unauthorized(settings, sslSettings, baseUrl).createService(VersionApi::class.java)
    }

    fun userApiWithToken(settings: Settings): UserApi {
        return clientToken(settings).createService(UserApi::class.java)
    }

    private fun defaultClient(
        authentications: Array<String>,
        settings: Settings,
        sslSettings: SSLSettings = settings.sslSettings(),
        baseUrl: String = settings.url
    ): ApiClient {
        val client = ApiClient(authentications)
        if (settings.legacyCert != null) {
            Logger.info("Migrating legacy CA cert to new location")
            var legacyCert: String? = null
            try {
                legacyCert = settings.legacyCert
                settings.legacyCert = null
                val caCertFile = File(settings.filesDir, CertUtils.CA_CERT_NAME)
                FileOutputStream(caCertFile).use {
                    it.write(legacyCert?.encodeToByteArray())
                }
                settings.caCertPath = caCertFile.absolutePath
                Logger.info("Migration of legacy CA cert succeeded")
            } catch (e: IOException) {
                Logger.error(e, "Migration of legacy CA cert failed")
                if (legacyCert != null) settings.legacyCert = legacyCert
            }
        }
        CertUtils.applySslSettings(client.okBuilder, sslSettings)
        client.adapterBuilder.baseUrl("$baseUrl/")
        return client
    }
}
