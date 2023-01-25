package com.github.gotify.api

import com.github.gotify.SSLSettings
import com.github.gotify.Settings
import com.github.gotify.client.ApiClient
import com.github.gotify.client.api.UserApi
import com.github.gotify.client.api.VersionApi
import com.github.gotify.client.auth.ApiKeyAuth
import com.github.gotify.client.auth.HttpBasicAuth

internal object ClientFactory {
    private fun unauthorized(baseUrl: String, sslSettings: SSLSettings): ApiClient {
        return defaultClient(arrayOf(), "$baseUrl/", sslSettings)
    }

    fun basicAuth(
        baseUrl: String,
        sslSettings: SSLSettings,
        username: String,
        password: String
    ): ApiClient {
        val client = defaultClient(
            arrayOf("basicAuth"),
            "$baseUrl/",
            sslSettings
        )
        val auth = client.apiAuthorizations["basicAuth"] as HttpBasicAuth
        auth.username = username
        auth.password = password
        return client
    }

    fun clientToken(
        baseUrl: String,
        sslSettings: SSLSettings,
        token: String?
    ): ApiClient {
        val client = defaultClient(
            arrayOf("clientTokenHeader"),
            "$baseUrl/",
            sslSettings
        )
        val tokenAuth = client.apiAuthorizations["clientTokenHeader"] as ApiKeyAuth
        tokenAuth.apiKey = token
        return client
    }

    fun versionApi(baseUrl: String, sslSettings: SSLSettings): VersionApi {
        return unauthorized(baseUrl, sslSettings).createService(VersionApi::class.java)
    }

    fun userApiWithToken(settings: Settings): UserApi {
        return clientToken(settings.url, settings.sslSettings(), settings.token)
            .createService(UserApi::class.java)
    }

    private fun defaultClient(
        authentications: Array<String>,
        baseUrl: String,
        sslSettings: SSLSettings
    ): ApiClient {
        val client = ApiClient(authentications)
        CertUtils.applySslSettings(client.okBuilder, sslSettings)
        client.adapterBuilder.baseUrl(baseUrl)
        return client
    }
}
