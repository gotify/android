package com.github.gotify.api;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;

public class Api {
    public static <T> T execute(Call<T> call) throws ApiException {
        try {
            Response<T> response = call.execute();

            if (response.isSuccessful()) {
                return response.body();
            } else {
                throw new ApiException(response);
            }
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }
}
