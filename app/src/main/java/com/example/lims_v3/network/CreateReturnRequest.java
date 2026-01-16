package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

public class CreateReturnRequest {
    @SerializedName("lend_ulid")
    private String lendUlid;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("processed_by_id")
    private String processedById;

    @SerializedName("note")
    private String note;

    public CreateReturnRequest(int quantity, String lendUlid, String processedById) {
        this.lendUlid = lendUlid;
        this.processedById = processedById;
        this.quantity = quantity;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
