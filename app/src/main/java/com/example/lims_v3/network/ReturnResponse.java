package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

import java.sql.Time;

public class ReturnResponse {
    @SerializedName("return_ulid")
    private String rturnUlid;

    @SerializedName("lend_ulid")
    private String lendUlid;

    @SerializedName("quantity")
    private int quantity;

    @SerializedName("processed_by_id")
    private String proceddedById;

    @SerializedName("returned_at")
    private Time returnedAt;

    @SerializedName("note")
    private String note;

}
