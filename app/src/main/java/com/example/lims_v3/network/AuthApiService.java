package com.example.lims_v3.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("login")
    Call<TokenResponse> login(@Body LoginRequest request);
}
