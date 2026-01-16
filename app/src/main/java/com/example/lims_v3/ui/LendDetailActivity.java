package com.example.lims_v3.ui;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;

public class LendDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // レイアウトファイルは適当に activity_lend_detail.xml を作ってください
        setContentView(R.layout.activity_lend_detail);

        // 渡されたIDを受け取る
        String lendUlid = getIntent().getStringExtra("LEND_ULID");

        if (lendUlid == null) {
            Toast.makeText(this, "エラー: IDが取得できませんでした", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 確認用トースト
        Toast.makeText(this, "詳細表示: " + lendUlid, Toast.LENGTH_SHORT).show();

        // TODO: ここで /lends/:lend_ulid APIを叩いて詳細を表示する
        // fetchLendDetail(lendUlid);

        // 戻るボタン
        findViewById(R.id.btnBackLendingHistoryDetails).setOnClickListener(v -> finish());
    }
}