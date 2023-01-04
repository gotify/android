package com.github.gotify.api

import java.io.IOException
import retrofit2.Call

internal object Api {
    @Throws(ApiException::class)
    fun <T> execute(call: Call<T>): T {
        try {
            val response = call.execute()

            if (response.isSuccessful) {
                return response.body() ?: throw ApiException("null response", response)
            } else {
                throw ApiException(response)
            }
        } catch (e: IOException) {
            throw ApiException(e)
        }
    }
}
