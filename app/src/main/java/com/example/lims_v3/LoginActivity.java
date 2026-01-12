package com.example.lims_v3;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.ui.MenuActivity;
import com.example.lims_v3.util.FeliCaReader;

public class LoginActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private EditText etUserId;
    private NfcAdapter nfcAdapter;
    private FeliCaReader feliCaReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // UI初期化
        etUserId = findViewById(R.id.etUserId);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnStudentCardAuth = findViewById(R.id.btnStudentCardAuth);

        // NFCアダプターとリーダーの初期化
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        feliCaReader = new FeliCaReader();

        if (nfcAdapter == null) {
            Toast.makeText(this, "このデバイスはNFCに対応していません", Toast.LENGTH_LONG).show();
            // 必要ならボタンを無効化
            btnStudentCardAuth.setEnabled(false);
        }

        // 通常ログインボタン
        btnLogin.setOnClickListener(v -> {
            String userId = etUserId.getText().toString();
            if (!userId.isEmpty()) {
                navigateToMenu(userId);
            } else {
                Toast.makeText(this, "IDを入力してください", Toast.LENGTH_SHORT).show();
            }
        });

        // 学生証認証ボタン
        // NFC ReaderModeはonResumeで有効になるため、ボタンは「ガイド表示」として使います
        btnStudentCardAuth.setOnClickListener(v -> {
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                Toast.makeText(this, "学生証を端末の背面にタッチしてください", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "NFC機能をONにしてください", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- NFC ライフサイクル制御 ---

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            // ReaderModeを有効化 (FeliCaのみ反応させ、システム音を鳴らす設定)
            Bundle options = new Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            nfcAdapter.enableReaderMode(
                    this,
                    this, // onTagDiscoveredが呼ばれます
                    NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    options
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            // アプリがバックグラウンドに行ったらReaderModeを解除
            nfcAdapter.disableReaderMode(this);
        }
    }

    // --- NFC 検知時のコールバック ---

    @Override
    public void onTagDiscovered(Tag tag) {
        // ここはバックグラウンドスレッドで呼ばれます
        feliCaReader.readStudentId(tag, new FeliCaReader.FeliCaCallback() {
            @Override
            public void onSuccess(@NonNull String studentId) {
                // UI操作のためメインスレッドに戻す [cite: 1]
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "読み取り成功: " + studentId, Toast.LENGTH_SHORT).show();
                    // ID欄を埋める場合
                    etUserId.setText(studentId);
                    // そのままログインさせる場合
                    navigateToMenu(studentId);
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "読み取り失敗: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("LoginNFC", "Error", exception);
                });
            }
        });
    }

    // --- 画面遷移 ---

    private void navigateToMenu(String userId) {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }
}