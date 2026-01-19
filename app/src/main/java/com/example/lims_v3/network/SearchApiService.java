package com.example.lims_v3.network;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SearchApiService {
    // GET /assets/search?name=xxx
    @GET("assets/search")
    Call<List<AssetSetResponse>> searchAssets(@Query("name") String name);
}