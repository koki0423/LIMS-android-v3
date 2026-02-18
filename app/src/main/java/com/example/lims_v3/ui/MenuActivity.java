package com.example.lims_v3.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.LoginActivity;
import com.example.lims_v3.R;

public class MenuActivity extends AppCompatActivity {
    private boolean isTerminalMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // ユーザー名表示
        String userId = getIntent().getStringExtra("USER_ID");
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("こんにちは、" + userId + "さん");

        // 設定（ターミナルモード）読み込み
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        isTerminalMode = prefs.getBoolean(SettingsActivity.KEY_TERMINAL_MODE, false);

        // 貸出ボタン
        Button btnLending = findViewById(R.id.btnLending);
        btnLending.setOnClickListener(v -> {
            Intent intent;

            if (isTerminalMode) {
                // ターミナル用（スキャナ入力）へ
                intent = new Intent(MenuActivity.this, LendingTerminalActivity.class);
            } else {
                // 既存（カメラターゲット）へ
                intent = new Intent(MenuActivity.this, LendingActivity.class);
            }

            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        // 貸出履歴ボタン
        Button btnLendHistory=findViewById(R.id.btnLendHistory);
        btnLendHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, LendHistoryActivity.class);
            startActivity(intent);
        });

        // 返却ボタン
        Button btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(v -> {
            Intent intent;

            if (isTerminalMode) {
                intent = new Intent(MenuActivity.this, ReturnTerminalActivity.class);
            } else {
                intent = new Intent(MenuActivity.this, ReturnActivity.class);
            }

            intent.putExtra("USER_ID", userId);
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
//        btnSearch.setOnClickListener(v -> {
//            Intent intent =new Intent(MenuActivity.this, SearchActivity.class);
//            startActivity(intent);
//        });
        btnSearch.setOnClickListener(v -> {
            Toast.makeText(MenuActivity.this,
                    "動作不安定のため今後機能追加予定です．",
                    Toast.LENGTH_SHORT).show();
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