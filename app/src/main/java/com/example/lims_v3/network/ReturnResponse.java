package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class ReturnResponse {
    @SerializedName("return_id")
    private long returnId;

    @SerializedName("return_ulid")
    private String returnUlid;

    @SerializedName("lend_id")
    private long lendId;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("processed_by_id")
    private String processedById;

    @SerializedName("returned_at")
    private Date returnedAt;

    @SerializedName("note")
    private String note;

    // Getter
    public String getReturnUlid() { return returnUlid; }
    public long getLendId() { return lendId; }
    public int getQuantity() { return quantity; }
    public Date getReturnedAt() { return returnedAt; }
    public String getProcessedById() { return processedById; }
    public String getNote() { return note; }
}