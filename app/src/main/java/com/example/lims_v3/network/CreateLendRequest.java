package com.example.lims_v3.network; // パッケージ名は適宜調整してください

import com.google.gson.annotations.SerializedName;

public class CreateLendRequest {
    // JSONのフィールド名("quantity")と合わせるためのアノテーション
    @SerializedName("quantity")
    private int quantity;

    @SerializedName("borrower_id")
    private String borrowerId;

    @SerializedName("lent_by_id")
    private String lentById;

    @SerializedName("note")
    private String note;

    // 必要に応じて due_on も追加可能

    public CreateLendRequest(int quantity, String borrowerId, String lentById) {
        this.quantity = quantity;
        this.borrowerId = borrowerId;
        this.lentById = lentById;
    }

    // Setter/Getter (必要に応じて)
    public void setNote(String note) {
        this.note = note;
    }
}