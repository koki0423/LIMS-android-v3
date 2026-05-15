package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

public class TokenResponse {
    @SerializedName("token")
    private String token;

    @SerializedName("message")
    private String message;

    public String getToken() {
        return token;
    }

    public String getMessage() {
        return message;
    }
}
