package com.example.lims_v3.ui;

import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.LendResponse;
import com.example.lims_v3.network.LendingApiService;
import com.example.lims_v3.network.ListLendsResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LendHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lending_history); // 以前作成したXMLを使用

        // 戻るボタン
        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());

        // コンテナ取得
        historyContainer = findViewById(R.id.historyContainer);

        // 履歴データ取得開始
        fetchLendHistory();
    }

    private void fetchLendHistory() {
        // 1. API URL取得
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");

        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "API URL未設定", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!baseUrl.endsWith("/")) baseUrl += "/";

//        // Gsonの設定を作成
//        Gson gson = new GsonBuilder()
//                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss") // Goの標準フォーマットに合わせる
//                .create();

        // 2. Retrofit準備
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        LendingApiService service = retrofit.create(LendingApiService.class);

        // 3. APIコール (全件取得)
        service.getLendHistory().enqueue(new Callback<List<LendResponse>>() {
            @Override
            public void onResponse(Call<List<LendResponse>> call, Response<List<LendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LendResponse> items = response.body();

                    if (items.isEmpty()) {
                        showEmptyMessage();
                    } else {
                        updateHistoryList(items);
                    }
                } else {
                    Toast.makeText(LendHistoryActivity.this, "取得失敗: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<LendResponse>> call, Throwable t) {
                // エラーログ出力
                Log.e("LendHistory", "通信エラー", t);
                Toast.makeText(LendHistoryActivity.this, "通信エラー", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateHistoryList(List<LendResponse> items) {
        historyContainer.removeAllViews(); // クリア

        // レイアウト読み込み用のInflater
        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

        for (LendResponse item : items) {
            // XMLからViewを生成 (CardView)
            View card = inflater.inflate(R.layout.item_lend_history, historyContainer, false);

            // 中身のTextViewを取得
            TextView tvNum = card.findViewById(R.id.tvItemManagementNumber);
            TextView tvBorrower = card.findViewById(R.id.tvItemBorrower);
            TextView tvDate = card.findViewById(R.id.tvItemDate);

            // データをセット
            tvNum.setText(item.getManagementNumber());
            tvBorrower.setText(item.getBorrowerId());

            if (item.getLentAt() != null) {
//                tvDate.setText(sdf.format(item.getLentAt()));
                tvDate.setText(item.getLentAt());
            } else {

                tvDate.setText("-");
            }

            // カード全体のクリックイベント
            card.setOnClickListener(v -> {
                // 1. 詳細画面へのIntentを作成
                Intent intent = new Intent(LendHistoryActivity.this, LendDetailActivity.class);

                // 2. どの貸出か特定するためのID (ULID) を渡す
                // ※LendResponseに getLendUlid() がある前提
                intent.putExtra("LEND_ULID", item.getLendUlid());

                // 3. 遷移！
                startActivity(intent);
            });

            // コンテナに追加
            historyContainer.addView(card);
        }
    }

    private void showEmptyMessage() {
        historyContainer.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("履歴がありません");
        tv.setPadding(32, 32, 32, 32);
        tv.setGravity(Gravity.CENTER);
        historyContainer.addView(tv);
    }
}