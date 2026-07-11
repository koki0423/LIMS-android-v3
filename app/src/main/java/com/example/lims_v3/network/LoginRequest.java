package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {
    @SerializedName("id")
    private final String id;

    @SerializedName("user_id")
    private final String userId;

    @SerializedName("password")
    private final String password;

    public LoginRequest(String userId, String password) {
        this.id = userId;
        this.userId = userId;
        this.password = password;
    }
}
