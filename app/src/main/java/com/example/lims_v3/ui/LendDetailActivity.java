package com.example.lims_v3.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.AssetMasterResponse;
import com.example.lims_v3.network.LendResponse;
import com.example.lims_v3.network.LendingApiService;
import com.example.lims_v3.util.ApiClientFactory;

import java.text.SimpleDateFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LendDetailActivity extends AppCompatActivity {

    private TextView tvAssetName;
    private TextView tvManagementNumber;
    private TextView tvBorrower;
    private TextView tvLentAt;
    private TextView tvNote;
    private TextView tvStatus;
    private LendingApiService lendingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lend_detail);

        tvAssetName = findViewById(R.id.tvDetailAssetName);
        tvManagementNumber = findViewById(R.id.tvDetailManagementNumber);
        tvBorrower = findViewById(R.id.tvDetailBorrower);
        tvLentAt = findViewById(R.id.tvDetailLentAt);
        tvNote = findViewById(R.id.tvDetailNote);
        tvStatus = findViewById(R.id.tvDetailStatus);

        Button btnBack = findViewById(R.id.btnBackLendingHistoryDetails);
        btnBack.setOnClickListener(v -> finish());

        String lendUlid = getIntent().getStringExtra("LEND_ULID");
        if (lendUlid == null) {
            Toast.makeText(this, "ID取得エラー", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            lendingService = ApiClientFactory.createService(this, LendingApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchDetail(lendUlid);
    }

    private void fetchDetail(String lendUlid) {
        lendingService.getLendDetail(lendUlid).enqueue(new Callback<LendResponse>() {
            @Override
            public void onResponse(Call<LendResponse> call, Response<LendResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bindData(response.body());
                } else {
                    tvManagementNumber.setText("データ取得失敗");
                    tvAssetName.setText("-");
                }
            }

            @Override
            public void onFailure(Call<LendResponse> call, Throwable t) {
                tvManagementNumber.setText("通信エラー: " + t.getMessage());
                tvAssetName.setText("-");
            }
        });
    }

    private void bindData(LendResponse item) {
        tvManagementNumber.setText(getDisplayText(item.getManagementNumber()));
        tvBorrower.setText(getDisplayText(item.getBorrowerId()));

        if (item.getLentAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());
            tvLentAt.setText(sdf.format(item.getLentAt()));
        } else {
            tvLentAt.setText("-");
        }

        String note = item.getNote();
        tvNote.setText(note != null && !note.trim().isEmpty() ? note.trim() : "(なし)");
        bindStatus(item.getReturned());
        fetchAssetName(item.getManagementNumber());
    }

    private void fetchAssetName(String managementNumber) {
        if (managementNumber == null || managementNumber.trim().isEmpty()) {
            tvAssetName.setText("-");
            return;
        }

        tvAssetName.setText("取得中...");
        lendingService.getAssetMaster(managementNumber).enqueue(new Callback<AssetMasterResponse>() {
            @Override
            public void onResponse(Call<AssetMasterResponse> call, Response<AssetMasterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String assetName = response.body().getName();
                    tvAssetName.setText(assetName != null && !assetName.trim().isEmpty()
                            ? assetName.trim()
                            : "備品名未設定");
                } else {
                    tvAssetName.setText("備品情報が見つかりません");
                }
            }

            @Override
            public void onFailure(Call<AssetMasterResponse> call, Throwable t) {
                tvAssetName.setText("通信エラー");
            }
        });
    }

    private void bindStatus(boolean returned) {
        if (returned) {
            tvStatus.setText("返却済み");
            tvStatus.setBackgroundResource(R.drawable.bg_lend_status_returned);
            tvStatus.setTextColor(0xFF424242);
            return;
        }

        tvStatus.setText("貸出中");
        tvStatus.setBackgroundResource(R.drawable.bg_lend_status_active);
        tvStatus.setTextColor(0xFF6D4C41);
    }

    private String getDisplayText(String value) {
        return value != null && !value.trim().isEmpty() ? value : "-";
    }
}
