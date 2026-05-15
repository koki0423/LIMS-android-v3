package com.example.lims_v3.network;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class GenreResponse implements Serializable {
    @SerializedName(value = "genre_id", alternate = {"id"})
    private long genreId;

    @SerializedName(value = "genre_code", alternate = {"code"})
    private String genreCode;

    @SerializedName(value = "genre_name", alternate = {"name"})
    private String genreName;

    @SerializedName("is_disabled")
    private boolean disabled;

    public long getGenreId() {
        return genreId;
    }

    public String getGenreCode() {
        return genreCode;
    }

    public String getGenreName() {
        return genreName;
    }

    public boolean isDisabled() {
        return disabled;
    }
}
