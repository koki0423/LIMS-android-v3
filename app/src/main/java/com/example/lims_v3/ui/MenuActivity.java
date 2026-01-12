package com.example.lims_v3.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.LoginActivity;
import com.example.lims_v3.R;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // ユーザー名表示 [cite: 29]
        String userId = getIntent().getStringExtra("USER_ID");
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("こんにちは、" + userId + "さん");

        // 貸出ボタン [cite: 28]
        Button btnLending = findViewById(R.id.btnLending);
        btnLending.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, LendingActivity.class);
            // ログイン時に受け取っていた USER_ID を次へ渡す
            intent.putExtra("USER_ID", getIntent().getStringExtra("USER_ID"));
            startActivity(intent);
        });

        // 貸出履歴ボタン
        Button btnLendHistory=findViewById(R.id.btnLendHistory);
        btnLendHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, LendingHistoryActivity.class);
            startActivity(intent);
        });

        // 返却ボタン
        Button btnReturn=findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ReturnActivity.class);
            startActivity(intent);
        });

        // 返却履歴ボタン
        Button btnReturnHistory=findViewById(R.id.btnReturnHistory);
        btnReturnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, ReturnHistoryActivity.class);
            startActivity(intent);
        });

        // 検索ボタン
        Button btnSearch=findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            Intent intent =new Intent(MenuActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        // 設定ボタン
        Button btnSettings =findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(v -> {
            Intent intent =new Intent(MenuActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // ログアウト
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}