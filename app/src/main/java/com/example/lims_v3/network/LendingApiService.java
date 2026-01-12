package com.example.lims_v3.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface LendingApiService {
    // :management_number の部分を @Path で置換します
    @POST("assets/{management_number}/lends")
    Call<Void> createLend(
            @Path("management_number") String managementNumber,
            @Body CreateLendRequest request
    );
    // 備品マスタ取得用
    @GET("assets/masters/{management_number}")
    Call<AssetMasterResponse> getAssetMaster(
            @Path("management_number") String managementNumber
    );
}