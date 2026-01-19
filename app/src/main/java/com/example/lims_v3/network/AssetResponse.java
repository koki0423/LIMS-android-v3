package com.example.lims_v3.network;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class AssetResponse implements Serializable {
    @SerializedName("asset_id")
    private long assetId;

    @SerializedName("management_number")
    private String managementNumber;

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

    @SerializedName("notes")
    private String notes;

    // Getter
    public String getManagementNumber() { return managementNumber; }
    public String getLocation() { return location; }
    public int getQuantity() { return quantity; }
    public int getStatusId() { return statusId; }
    public String getSerial() { return serial; }
    public String getPurchasedAt() { return purchasedAt; }
    public String getNotes() { return notes; }
}