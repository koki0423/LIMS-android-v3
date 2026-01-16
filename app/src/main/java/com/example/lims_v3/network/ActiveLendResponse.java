package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

public class ActiveLendResponse {
    @SerializedName("lend_ulid")
    private String lendUlid;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("borrower_id")
    private String borrowerId;

    // 必要に応じて他のフィールドも追加可能です
    // @SerializedName("lent_at")
    // private String lentAt;

    public String getLendUlid() {
        return lendUlid;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getBorrowerId() {
        return borrowerId;
    }
}