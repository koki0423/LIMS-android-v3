package com.example.lims_v3.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.AssetResponse;
import com.example.lims_v3.network.AssetMasterResponse;
import com.example.lims_v3.network.AssetSetResponse;

public class AssetDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_detail);

        findViewById(R.id.btnBackDetail).setOnClickListener(v -> finish());

        // Intentからオブジェクトを受け取る
        AssetSetResponse data = (AssetSetResponse) getIntent().getSerializableExtra("ASSET_DATA");

        if (data == null) {
            Toast.makeText(this, "データ取得エラー", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bindData(data);
    }

    private void bindData(AssetSetResponse data) {
        // --- 1. 製品情報 (Master) ---
        AssetMasterResponse master = data.getMaster();
        if (master != null) {
            setText(R.id.tvDetailName, master.getName());
            setText(R.id.tvDetailManufacturer, master.getManufacturer());
            setText(R.id.tvDetailModel, master.getModel());
        }

        // --- 2. 管理情報 (Asset) ---
        AssetResponse asset = data.getAsset();
        if (asset != null) {
            setText(R.id.tvDetailMngNum, asset.getManagementNumber());
            setText(R.id.tvDetailLocation, asset.getLocation());
            setText(R.id.tvDetailQuantity, String.valueOf(asset.getQuantity()));
            setText(R.id.tvDetailSerial, asset.getSerial());
            setText(R.id.tvDetailNotes, asset.getNotes());

            // ステータス表示の装飾
            TextView tvStatus = findViewById(R.id.tvDetailStatus);
            setupStatus(tvStatus, asset.getStatusId());
        }
    }

    // 安全にテキストをセットするヘルパーメソッド
    private void setText(int resId, String text) {
        TextView tv = findViewById(resId);
        if (text != null && !text.isEmpty()) {
            tv.setText(text);
        } else {
            tv.setText("-");
        }
    }

    // ステータスIDを文字と色に変換する
    private void setupStatus(TextView tv, int statusId) {
        String label;
        int color;

        // ※実際のDB定義に合わせて調整してください
        switch (statusId) {
            case 1: // 利用可能
                label = "利用可能";
                color = 0xFFC8E6C9; // 薄い緑
                break;
            case 2: // 故障/修理中
                label = "メンテナンス中";
                color = 0xFFFFCDD2; // 薄い赤
                break;
            case 3: // 修理中
                label = "メンテナンス中";
                color = 0xFFFFCDD2; // 薄い赤
                break;
            case 4: // 貸出中
                label = "貸出中";
                color = 0xFFFFCC80; // 薄いオレンジ
                break;
            case 5: // 廃棄済み
                label = "廃棄済み";
                color = 0xFFE0E0E0; // グレー
                break;
            default:
                label = "不明(" + statusId + ")";
                color = 0xFFFFFFFF;
                break;
        }

        tv.setText(label);
        tv.setBackgroundColor(color);
    }
}