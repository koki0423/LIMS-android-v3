package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

public class ReturnHistoryResponse {
    @SerializedName("items")
    private ReturnResponse[] items;
    @SerializedName("total")
    private int total;

    @SerializedName("next_offset")
    private int nextOffset;
}
