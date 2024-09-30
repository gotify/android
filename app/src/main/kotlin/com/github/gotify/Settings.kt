package com.github.gotify

import android.content.Context
import android.content.SharedPreferences
import com.github.gotify.client.model.User

internal class Settings(private val sharedPreferences: SharedPreferences) {
    var url: String
        get() = sharedPreferences.getString(KEY_URL, "")!!
        set(value) = sharedPreferences.edit().putString(KEY_URL, value).apply()
    var token: String?
        get() = sharedPreferences.getString(KEY_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_TOKEN, value).apply()
    var user: User? = null
        get() {
            val username = sharedPreferences.getString(KEY_USERNAME, null)
            val admin = sharedPreferences.getBoolean(KEY_ADMIN, false)
            return if (username != null) {
                User().name(username).admin(admin)
            } else {
                User().name("UNKNOWN").admin(false)
            }
        }
        private set
    var serverVersion: String
        get() = sharedPreferences.getString(KEY_VERSION, "UNKNOWN")!!
        set(value) = sharedPreferences.edit().putString(KEY_VERSION, value).apply()
    var legacyCert: String?
        get() = sharedPreferences.getString(KEY_CERTIFICATE, null)
        set(value) = sharedPreferences.edit().putString(KEY_CERTIFICATE, value).commit().toUnit()
    var caCertPath: String?
        get() = sharedPreferences.getString(KEY_CA_CERTIFICATE_PATH, null)
        set(value) = sharedPreferences.edit().putString(
            KEY_CA_CERTIFICATE_PATH,
            value
        ).commit().toUnit()
    var validateSSL: Boolean
        get() = sharedPreferences.getBoolean(KEY_VALIDATE_SSL, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_VALIDATE_SSL, value).apply()
    var clientCertPath: String?
        get() = sharedPreferences.getString(KEY_CLIENT_CERTIFICATE_PATH, null)
        set(value) = sharedPreferences.edit().putString(KEY_CLIENT_CERTIFICATE_PATH, value).apply()
    var clientCertPassword: String?
        get() = sharedPreferences.getString(KEY_CLIENT_CERTIFICATE_PASS, null)
        set(value) = sharedPreferences.edit().putString(KEY_CLIENT_CERTIFICATE_PASS, value).apply()

    fun tokenExists(): Boolean = !token.isNullOrEmpty()

    fun clear() {
        url = ""
        token = null
        validateSSL = true
        legacyCert = null
        caCertPath = null
        clientCertPath = null
        clientCertPassword = null
    }

    fun setUser(name: String?, admin: Boolean) {
        sharedPreferences.edit().putString(KEY_USERNAME, name).putBoolean(KEY_ADMIN, admin).apply()
    }

    fun sslSettings(): SSLSettings {
        return SSLSettings(
            validateSSL,
            caCertPath,
            clientCertPath,
            clientCertPassword
        )
    }

    @Suppress("UnusedReceiverParameter")
    private fun Any?.toUnit() = Unit

    companion object {
        private const val KEY_URL = "url"
        private const val KEY_TOKEN = "token"
        private const val KEY_USERNAME = "username"
        private const val KEY_ADMIN = "admin"
        private const val KEY_VERSION = "version"
        private const val KEY_CERTIFICATE = "cert"
        private const val KEY_CA_CERTIFICATE_PATH = "caCertPath"
        private const val KEY_VALIDATE_SSL = "validateSSL"
        private const val KEY_CLIENT_CERTIFICATE_PATH = "clientCertPath"
        private const val KEY_CLIENT_CERTIFICATE_PASS = "clientCertPass"

        operator fun invoke(context: Context): Settings = Settings(
            sharedPreferences = context.getSharedPreferences("gotify", Context.MODE_PRIVATE)
        )
    }
}
