package com.example.lims_v3.network;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AssetMasterResponse implements Serializable {
    @SerializedName("asset_master_id")
    private long assetMasterId;

    @SerializedName("name")
    private String name;

    @SerializedName("manufacturer")
    private String manufacturer;

    @SerializedName("model")
    private String model;

    // Getter
    public String getName() { return name; }
    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
}