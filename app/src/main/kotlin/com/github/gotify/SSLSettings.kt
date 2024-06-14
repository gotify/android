package com.github.gotify

internal data class SSLSettings(
    val validateSSL: Boolean,
    val caCertPath: String?,
    val clientCertPath: String?,
    val clientCertPassword: String?
)
