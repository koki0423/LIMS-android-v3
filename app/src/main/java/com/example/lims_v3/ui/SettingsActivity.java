package com.example.lims_v3.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;

public class SettingsActivity extends AppCompatActivity {

    // SharedPreferencesのファイル名とキーの定数定義
    // 他のActivity（ログイン画面や貸出画面）から呼び出すときもこのキーを使います
    public static final String KEY_TERMINAL_MODE = "TERMINAL_MODE";

    public static final String PREF_NAME = "LendingAppPrefs";
    public static final String KEY_API_URL = "API_URL";

    private EditText etApiUrl;
    private CheckBox cbTerminalMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // UI部品の取得
        etApiUrl = findViewById(R.id.etApiUrl);
        cbTerminalMode = findViewById(R.id.cbTerminalMode);
        Button btnBack = findViewById(R.id.btnBackSettings);
        Button btnSave = findViewById(R.id.btnSaveSettings);


        // 1. 保存されている設定値を読み込んで表示
        loadSettings();

        // 戻るボタン
        btnBack.setOnClickListener(v -> finish());

        // 保存ボタン
        btnSave.setOnClickListener(v -> {
            saveSettings();
        });

        // NFC動作テスト
        findViewById(R.id.btnTestNfc).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, NfcTestActivity.class);
            startActivity(intent);
        });

        // カメラ動作テスト
        findViewById(R.id.btnTestCamera).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, CameraTestActivity.class);
            startActivity(intent);
        });
    }

    // 設定を読み込むメソッド
    private void loadSettings() {
        // SharedPreferencesを取得 (MODE_PRIVATEはこのアプリからのみアクセス可能)
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // 保存されたURLを取得。保存されていない場合はデフォルト値を返す
        // デフォルト値例: "http://192.168.0.1:8080/api/v2"
        String savedUrl = prefs.getString(KEY_API_URL, "");

        // EditTextにセット
        etApiUrl.setText(savedUrl);

        boolean savedTerminalMode = prefs.getBoolean(KEY_TERMINAL_MODE, false);
        cbTerminalMode.setChecked(savedTerminalMode);
    }

    // 設定を保存するメソッド
    private void saveSettings() {
        String url = etApiUrl.getText().toString().trim();
        boolean isTerminalMode = cbTerminalMode.isChecked();

        if (url.isEmpty()) {
            Toast.makeText(this, "URLを入力してください", Toast.LENGTH_SHORT).show();
            return;
        }

        // SharedPreferencesに書き込み
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_API_URL, url);
        editor.putBoolean(KEY_TERMINAL_MODE, isTerminalMode);
        editor.apply(); // apply()は非同期で保存するのでUIをブロックしない

        Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show();

        // 保存したら前の画面に戻るか、そのままにするかは要件次第（ここではそのまま）
        // finish();
    }
}