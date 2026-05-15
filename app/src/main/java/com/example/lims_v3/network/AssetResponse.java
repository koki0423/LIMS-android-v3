package com.example.lims_v3.network;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AssetResponse implements Serializable {
    @SerializedName("asset_id")
    private long assetId;

    @SerializedName("asset_master_id")
    private long assetMasterId;

    @SerializedName("management_number")
    private String managementNumber;

    @SerializedName("name")
    private String name;

    @SerializedName("location")
    private String location;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("status_id")
    private int statusId;

    @SerializedName("serial")
    private String serial;

    @SerializedName("purchased_at")
    private String purchasedAt; // 日付文字列

    @SerializedName("owner")
    private String owner;

    @SerializedName("default_location")
    private String defaultLocation;

    @SerializedName("last_checked_at")
    private String lastCheckedAt;

    @SerializedName("last_checked_by")
    private String lastCheckedBy;

    @SerializedName("notes")
    private String notes;

    public AssetResponse() {
    }

    public AssetResponse(
            long assetId,
            String managementNumber,
            String location,
            int quantity,
            int statusId,
            String serial,
            String purchasedAt,
            String notes
    ) {
        this.assetId = assetId;
        this.managementNumber = managementNumber;
        this.location = location;
        this.quantity = quantity;
        this.statusId = statusId;
        this.serial = serial;
        this.purchasedAt = purchasedAt;
        this.notes = notes;
    }

    // Getter
    public long getAssetId() { return assetId; }
    public long getAssetMasterId() { return assetMasterId; }
    public String getManagementNumber() { return managementNumber; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public int getQuantity() { return quantity; }
    public int getStatusId() { return statusId; }
    public String getSerial() { return serial; }
    public String getPurchasedAt() { return purchasedAt; }
    public String getOwner() { return owner; }
    public String getDefaultLocation() { return defaultLocation; }
    public String getLastCheckedAt() { return lastCheckedAt; }
    public String getLastCheckedBy() { return lastCheckedBy; }
    public String getNotes() { return notes; }
}
