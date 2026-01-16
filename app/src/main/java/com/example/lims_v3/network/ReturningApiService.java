package com.example.lims_v3.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ReturningApiService {
    @GET("assets/active-lend/{management_number}")
    Call<ActiveLendResponse> getActiveLend(
            @Path("management_number") String managementNumber
    );

    // 2. 返却を実行する
    @POST("returns")
    Call<Void> createReturn(
            @Body CreateReturnRequest request
    );

    // 返却履歴全てを取得
    @GET("/returns")
    Call<ReturnHistoryResponse> getReturnHistory(

    );

}