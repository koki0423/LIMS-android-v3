package com.example.lims_v3.network;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;

public class AssetMasterResponse implements Serializable {
    @SerializedName("asset_master_id")
    private long assetMasterId;

    @SerializedName("management_number")
    private String managementNumber;

    @SerializedName("name")
    private String name;

    @SerializedName("management_category_id")
    private long managementCategoryId;

    @SerializedName("manufacturer")
    private String manufacturer;

    @SerializedName("model")
    private String model;

    @SerializedName("genre")
    private String genre;

    @SerializedName("genre_id")
    private Long genreId;

    @SerializedName("genre_name")
    private String genreName;

    @SerializedName("created_at")
    private Date createdAt;

    public AssetMasterResponse() {
    }

    public AssetMasterResponse(long assetMasterId, String name, String manufacturer, String model, String genre) {
        this.assetMasterId = assetMasterId;
        this.name = name;
        this.manufacturer = manufacturer;
        this.model = model;
        this.genre = genre;
    }

    // Getter
    public long getAssetMasterId() { return assetMasterId; }
    public String getManagementNumber() { return managementNumber; }
    public String getName() { return name; }
    public long getManagementCategoryId() { return managementCategoryId; }
    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public String getGenre() { return genre; }
    public Long getGenreId() { return genreId; }
    public String getGenreName() {
        if (genreName != null && !genreName.isEmpty()) {
            return genreName;
        }
        return genre;
    }
    public Date getCreatedAt() { return createdAt; }
}
