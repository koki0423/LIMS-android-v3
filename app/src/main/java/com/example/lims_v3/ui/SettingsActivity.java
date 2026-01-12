package com.example.lims_v3.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;

public class SettingsActivity extends AppCompatActivity {

    // SharedPreferencesのファイル名とキーの定数定義
    // 他のActivity（ログイン画面や貸出画面）から呼び出すときもこのキーを使います
    public static final String PREF_NAME = "LendingAppPrefs";
    public static final String KEY_API_URL = "API_URL";

    private EditText etApiUrl;
    private TextView tvQrResult; // テスト用
    private TextView tvNfcResult; // テスト用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // UI部品の取得
        etApiUrl = findViewById(R.id.etApiUrl);
        tvQrResult = findViewById(R.id.tvQrResult);
        tvNfcResult = findViewById(R.id.tvNfcResult);
        Button btnBack = findViewById(R.id.btnBackSettings);
        Button btnSave = findViewById(R.id.btnSaveSettings);
        Button btnTestQr = findViewById(R.id.btnTestQr);
        Button btnTestNfc = findViewById(R.id.btnTestNfc);

        // 1. 保存されている設定値を読み込んで表示
        loadSettings();

        // 戻るボタン
        btnBack.setOnClickListener(v -> finish());

        // 保存ボタン
        btnSave.setOnClickListener(v -> {
            saveSettings();
        });

        // QR読み取りテスト（モック動作）
        btnTestQr.setOnClickListener(v -> {
            // TODO: ここにQR読み取りロジックを入れる（必要であればLendingActivityと同様の実装）
            tvQrResult.setText("読み取り成功: TEST-QR-001");
        });

        // NFC読み取りテスト（モック動作）
        btnTestNfc.setOnClickListener(v -> {
            // TODO: ここにNFC読み取りロジックを入れる
            tvNfcResult.setText("読み取り成功: IDm=0123456789ABCDEF");
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
    }

    // 設定を保存するメソッド
    private void saveSettings() {
        String url = etApiUrl.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "URLを入力してください", Toast.LENGTH_SHORT).show();
            return;
        }

        // SharedPreferencesに書き込み
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(KEY_API_URL, url);
        editor.apply(); // apply()は非同期で保存するのでUIをブロックしない

        Toast.makeText(this, "設定を保存しました", Toast.LENGTH_SHORT).show();

        // 保存したら前の画面に戻るか、そのままにするかは要件次第（ここではそのまま）
        // finish();
    }
}