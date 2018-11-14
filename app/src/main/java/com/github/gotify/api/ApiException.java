package com.github.gotify.api;

import java.io.IOException;
import java.util.Locale;
import retrofit2.Response;

public final class ApiException extends Exception {

    private String body;
    private int code;

    ApiException(Response<?> response) {
        super("Api Error", null);
        try {
            this.body = response.errorBody() != null ? response.errorBody().string() : "";
        } catch (IOException e) {
            this.body = "Error while getting error body :(";
        }
        this.code = response.code();
    }

    ApiException(Throwable cause) {
        super("Request failed.", cause);
        this.body = "";
        this.code = 0;
    }

    public String body() {
        return body;
    }

    public int code() {
        return code;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ENGLISH,
                "Code(%d) Response: %s",
                code(),
                body().substring(0, Math.min(body().length(), 200)));
    }
}
