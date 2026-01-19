package com.example.lims_v3.network;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AssetSetResponse implements Serializable {
    @SerializedName("master")
    private AssetMasterResponse master;

    @SerializedName("asset")
    private AssetResponse asset;

    public AssetMasterResponse getMaster() { return master; }
    public AssetResponse getAsset() { return asset; }
}