package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class LendResponse {
    @SerializedName("lend_ulid")
    private String lendUlid;

    @SerializedName("management_number")
    private String managementNumber;

    @SerializedName("borrower_id")
    private String borrowerId;

    @SerializedName("lent_at")
    private Date lentAt; // GsonがISO8601形式を自動パースしてくれます

    @SerializedName("returned")
    private boolean returned;

    // 必要に応じて他のフィールドも追加
    // @SerializedName("asset_master_id") private Long assetMasterId;
    // @SerializedName("quantity") private int quantity;

    public String getManagementNumber() {
        return managementNumber;
    }

    public String getBorrowerId() {
        return borrowerId;
    }

    public Date getLentAt() {
        return lentAt;
    }

    public String getLendUlid(){
        return lendUlid;
    }
}