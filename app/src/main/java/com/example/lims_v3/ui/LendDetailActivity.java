package com.example.lims_v3.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.LendResponse;
import com.example.lims_v3.network.LendingApiService;

import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LendDetailActivity extends AppCompatActivity {

    // UIパーツ
    private TextView tvManagementNumber;
    private TextView tvBorrower;
    private TextView tvLentAt;
    private TextView tvNote;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lend_detail);

        // UI初期化
        tvManagementNumber = findViewById(R.id.tvDetailManagementNumber);
        tvBorrower = findViewById(R.id.tvDetailBorrower);
        tvLentAt = findViewById(R.id.tvDetailLentAt);
        tvNote = findViewById(R.id.tvDetailNote);
        tvStatus = findViewById(R.id.tvDetailStatus);

        Button btnBack = findViewById(R.id.btnBackLendingHistoryDetails);
        btnBack.setOnClickListener(v -> finish());

        // 前画面からIDを受け取る
        String lendUlid = getIntent().getStringExtra("LEND_ULID");

        if (lendUlid == null) {
            Toast.makeText(this, "ID取得エラー", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // APIデータ取得
        fetchDetail(lendUlid);
    }

    private void fetchDetail(String lendUlid) {
        // 1. URL取得
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // 2. Retrofit準備
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        LendingApiService service = retrofit.create(LendingApiService.class);

        // 3. APIコール
        service.getLendDetail(lendUlid).enqueue(new Callback<LendResponse>() {
            @Override
            public void onResponse(Call<LendResponse> call, Response<LendResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindData(response.body());
                } else {
                    tvManagementNumber.setText("データ取得失敗");
                }
            }

            @Override
            public void onFailure(Call<LendResponse> call, Throwable t) {
                tvManagementNumber.setText("通信エラー: " + t.getMessage());
            }
        });
    }

    // 取得したデータを画面にセットする処理
    private void bindData(LendResponse item) {
        tvManagementNumber.setText(item.getManagementNumber());
        tvBorrower.setText(item.getBorrowerId());

        // 日付フォーマット
        if (item.getLentAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            tvLentAt.setText(sdf.format(item.getLentAt()));
        }

        // 備考
         tvNote.setText(item.getNote() != null ? item.getNote() : "(なし)");

        // ステータス表示
        if (item.getReturned()) {
            tvStatus.setText("返却済み");
            tvStatus.setBackgroundColor(0xFFE0E0E0); // グレー
        } else {
            tvStatus.setText("貸出中");
            tvStatus.setBackgroundColor(0xFFFFCC80); // 薄いオレンジ
        }
    }
}