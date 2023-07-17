package com.github.gotify.api

import java.io.IOException
import retrofit2.Response

internal class ApiException : Exception {
    var body: String = ""
        private set
    var code: Int
        private set

    constructor(response: Response<*>) : super("Api Error", null) {
        body = try {
            if (response.errorBody() != null) response.errorBody()!!.string() else ""
        } catch (e: IOException) {
            "Error while getting error body :("
        }
        code = response.code()
    }

    constructor(exceptionBody: String, response: Response<*>) : super("Api Error", null) {
        body = exceptionBody
        code = response.code()
    }

    constructor(cause: Throwable?) : super("Request failed.", cause) {
        code = 0
    }

    override fun toString() = "Code($code) Response: ${body.take(200)}"
}
