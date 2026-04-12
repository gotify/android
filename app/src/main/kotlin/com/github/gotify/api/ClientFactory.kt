package com.github.gotify.api

import com.github.gotify.CfAccessSettings
import com.github.gotify.SSLSettings
import com.github.gotify.Settings
import com.github.gotify.client.ApiClient
import com.github.gotify.client.api.UserApi
import com.github.gotify.client.api.VersionApi
import com.github.gotify.client.auth.ApiKeyAuth
import com.github.gotify.client.auth.HttpBasicAuth

internal object ClientFactory {
    private fun unauthorized(
        settings: Settings,
        sslSettings: SSLSettings,
        baseUrl: String,
        cfAccessSettings: CfAccessSettings = settings.cfAccessSettings()
    ): ApiClient {
        return defaultClient(arrayOf(), settings, sslSettings, baseUrl, cfAccessSettings)
    }

    fun basicAuth(
        settings: Settings,
        sslSettings: SSLSettings,
        username: String,
        password: String,
        cfAccessSettings: CfAccessSettings = settings.cfAccessSettings()
    ): ApiClient {
        val client = defaultClient(
            arrayOf("basicAuth"), settings, sslSettings,
            cfAccessSettings = cfAccessSettings
        )
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
        baseUrl: String = settings.url,
        cfAccessSettings: CfAccessSettings = settings.cfAccessSettings()
    ): VersionApi {
        return unauthorized(settings, sslSettings, baseUrl, cfAccessSettings)
            .createService(VersionApi::class.java)
    }

    fun userApiWithToken(settings: Settings): UserApi {
        return clientToken(settings).createService(UserApi::class.java)
    }

    private fun defaultClient(
        authentications: Array<String>,
        settings: Settings,
        sslSettings: SSLSettings = settings.sslSettings(),
        baseUrl: String = settings.url,
        cfAccessSettings: CfAccessSettings = settings.cfAccessSettings()
    ): ApiClient {
        val client = ApiClient(authentications)
        CertUtils.applySslSettings(client.okBuilder, sslSettings)
        if (cfAccessSettings.enabled) {
            client.okBuilder.addInterceptor(
                CloudflareAccessInterceptor(
                    cfAccessSettings.clientId,
                    cfAccessSettings.clientSecret
                )
            )
        }
        client.adapterBuilder.baseUrl("$baseUrl/")
        return client
    }
}
