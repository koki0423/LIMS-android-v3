package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

public class AssetMasterResponse {
    @SerializedName("name")
    private String name;

    // 必要に応じて他のフィールドも追加可能です
    // @SerializedName("manufacturer")
    // private String manufacturer;

    public String getName() {
        return name;
    }
}