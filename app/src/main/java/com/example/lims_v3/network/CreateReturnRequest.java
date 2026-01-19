package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

public class CreateReturnRequest {
    @SerializedName("lend_id")
    private  String lend_id;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("processed_by_id")
    private String processedById;

    @SerializedName("note")
    private String note;

    public CreateReturnRequest(int quantity, String processedById) {
//        this.lend_id=lend_id;
        this.quantity = quantity;
        this.processedById = processedById;
    }

    public void setNote(String note) {
        this.note = note;
    }
}