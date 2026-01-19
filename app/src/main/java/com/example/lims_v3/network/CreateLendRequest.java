package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

public class CreateLendRequest {
    @SerializedName("management_number")
    private String management_number;
    @SerializedName("quantity")
    private int quantity;

    @SerializedName("borrower_id")
    private String borrowerId;

    @SerializedName("lent_by_id")
    private String lentById;

    @SerializedName("note")
    private String note;

    public CreateLendRequest(String management_number,int quantity, String borrowerId, String lentById) {
        this.management_number=management_number;
        this.quantity = quantity;
        this.borrowerId = borrowerId;
        this.lentById = lentById;
    }

    public void setNote(String note) {
        this.note = note;
    }
}