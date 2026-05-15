package com.example.lims_v3.network;

import java.util.Map;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface SearchApiService {
    @GET("assets/search")
    Call<List<AssetSetResponse>> searchAssets(@QueryMap Map<String, String> queryMap);
}
