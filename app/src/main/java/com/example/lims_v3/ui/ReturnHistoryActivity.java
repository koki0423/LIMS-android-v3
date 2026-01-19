package com.example.lims_v3.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.ReturnResponse;
import com.example.lims_v3.network.ReturningApiService;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ReturnHistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_lending_history.xml を使い回す
        setContentView(R.layout.activity_lending_history);

        // タイトル変更
        TextView title = findViewById(R.id.tvHistoryTitle);
        if (title != null) title.setText("返却履歴");

        // 戻るボタン
        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());
        historyContainer = findViewById(R.id.historyContainer);

        fetchHistory();
    }

    private void fetchHistory() {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // ★ 修正点: GsonBuilderを使わず、LendHistoryActivityと同じデフォルト設定にする
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ReturningApiService service = retrofit.create(ReturningApiService.class);

        service.getReturnHistory().enqueue(new Callback<List<ReturnResponse>>() {
            @Override
            public void onResponse(Call<List<ReturnResponse>> call, Response<List<ReturnResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ReturnResponse> items = response.body();
                    if (items.isEmpty()) {
                        showEmptyMessage();
                    } else {
                        updateList(items);
                    }
                } else {
                    Toast.makeText(ReturnHistoryActivity.this, "取得失敗: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ReturnResponse>> call, Throwable t) {
                Log.e("ReturnHistory", "通信エラー", t);
                Toast.makeText(ReturnHistoryActivity.this, "通信エラー", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateList(List<ReturnResponse> items) {
        historyContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        // LendHistoryActivityと同じフォーマットを使用
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

        for (ReturnResponse item : items) {
            View card = inflater.inflate(R.layout.item_return_history, historyContainer, false);

            TextView tvUlid = card.findViewById(R.id.tvReturnUlid);
            TextView tvDate = card.findViewById(R.id.tvReturnDate);
            TextView tvQty = card.findViewById(R.id.tvReturnQuantity);
            TextView tvProcessed = card.findViewById(R.id.tvProcessedBy);
            TextView tvNote = card.findViewById(R.id.tvReturnNote);

            // データをセット
            tvUlid.setText(item.getReturnUlid());
            tvQty.setText(String.valueOf(item.getQuantity()));
            tvProcessed.setText(item.getProcessedById());

            // ★ 修正点: Date型の場合は直接setTextできないため、必ずフォーマットする
            if (item.getReturnedAt() != null) {
                tvDate.setText(sdf.format(item.getReturnedAt()));
            } else {
                tvDate.setText("-");
            }

            // メモの表示制御
            if (item.getNote() != null && !item.getNote().isEmpty()) {
                tvNote.setText(item.getNote());
                tvNote.setVisibility(View.VISIBLE);
            } else {
                tvNote.setVisibility(View.GONE);
            }

            // クリック時の動作
            card.setOnClickListener(v -> {
                // 詳細画面への遷移が必要になったらコメントアウトを解除してください
                // Intent intent = new Intent(ReturnHistoryActivity.this, ReturnDetailActivity.class);
                // intent.putExtra("RETURN_ULID", item.getReturnUlid());
                // startActivity(intent);

                // 現状はトーストでID表示のみ
                Toast.makeText(ReturnHistoryActivity.this, "詳細: " + item.getReturnUlid(), Toast.LENGTH_SHORT).show();
            });

            historyContainer.addView(card);
        }
    }

    private void showEmptyMessage() {
        historyContainer.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("履歴がありません");
        tv.setPadding(32, 32, 32, 32);
        // Gravityの設定には定数クラスが必要な場合がありますが、setTextAlignment等でも代用可能です
        // tv.setGravity(android.view.Gravity.CENTER);
        historyContainer.addView(tv);
    }
}