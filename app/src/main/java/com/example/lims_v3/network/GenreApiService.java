package com.example.lims_v3.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GenreApiService {
    // Assumes the genre master endpoint is exposed as GET /genres.
    @GET("genres")
    Call<List<GenreResponse>> getGenres();
}
