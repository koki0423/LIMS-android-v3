package com.example.lims_v3.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface LendingApiService {
    // :management_number の部分を @Path で置換します
    @POST("lends")
    Call<Void> createLend(
//            @Path("management_number") String managementNumber,
            @Body CreateLendRequest request
    );
    // 備品マスタ取得用
    @GET("assets/masters/{management_number}")
    Call<AssetMasterResponse> getAssetMaster(
            @Path("management_number") String managementNumber
    );

    // 貸出履歴一覧取得
    // クエリパラメータが必要な場合は @Query("limit") int limit 等を追加可能
    @GET("lends")
    Call<List<LendResponse>> getLendHistory();

    // 特定の貸出詳細を取得
    @GET("lends/{lend_ulid}")
    Call<LendResponse> getLendDetail(
            @Path("lend_ulid") String lendUlid
    );
}