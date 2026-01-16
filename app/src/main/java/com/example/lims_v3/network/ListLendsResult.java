package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ListLendsResult {
    @SerializedName("items")
    private List<LendResponse> items;

    @SerializedName("total")
    private long total;

    @SerializedName("next_offset")
    private int nextOffset;

    public List<LendResponse> getItems() {
        return items;
    }
}