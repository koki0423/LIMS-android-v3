package com.example.lims_v3.network;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReturningApiService {

    // 1. 管理番号から「貸出中」のデータを検索
    // GET /lends?management_number=xxx&only_outstanding=true
    @GET("lends")
    Call<List<LendResponse>> searchActiveLend(
            @Query("management_number") String managementNumber,
            @Query("only_outstanding") boolean onlyOutstanding
    );

    // 2. 返却を実行
    // POST /return
    @POST("returns/key/{lend_key}")
    Call<Void> createReturn(
            @Path("lend_key") String lendKey,
            @Body CreateReturnRequest request
    );

    // 返却履歴一覧 (今回の変更で影響なし、そのまま維持)
    // GET /lends/:lend_ulid/returns の実装が必要ならここに追加
    @GET("returns")
    Call<ReturnHistoryResponse> getReturnHistory();
}