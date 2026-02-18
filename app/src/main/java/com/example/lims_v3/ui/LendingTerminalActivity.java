package com.example.lims_v3.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.CreateLendRequest;
import com.example.lims_v3.network.LendingApiService;
import com.example.lims_v3.util.FeliCaReader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LendingTerminalActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    // UIコンポーネント
    private EditText etEquipmentCode;
    private EditText etBorrowerId;
    private TextView tvActionResult;
    private Button btnExecute;

    // NFC / FeliCa
    private NfcAdapter nfcAdapter;
    private FeliCaReader feliCaReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lending_terminal);

        // --- UIの紐付け ---
        etEquipmentCode = findViewById(R.id.etEquipmentCode);
        etBorrowerId = findViewById(R.id.etBorrowerId);
        tvActionResult = findViewById(R.id.tvActionResult);
        btnExecute = findViewById(R.id.btnExecute);

        // 初期表示設定
        tvActionResult.setVisibility(View.GONE);

        // 戻るボタン
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // 実行ボタン
        btnExecute.setOnClickListener(v -> executeLend());

        // --- ハンディ端末向け設定 ---
        // 画面を開いた瞬間にソフトキーボードが出ないようにする
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // フォーカスが当たってもソフトキーボードを出さない（ハードウェアキー/スキャナ入力前提）
        etEquipmentCode.setShowSoftInputOnFocus(false);
        etBorrowerId.setShowSoftInputOnFocus(false);

        // 1. 備品番号入力欄の挙動設定
        // スキャナが「コード + Enter」を入力してきた時の処理
        etEquipmentCode.setOnEditorActionListener((v, actionId, event) -> {
            // Enterキー または ActionDone が押された場合
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                // 入力があれば、次の「貸出先」欄へフォーカスを移動
                if (v.getText().length() > 0) {
                    etBorrowerId.requestFocus();
                }
                return true; // イベント消費（改行文字を入れさせない）
            }
            return false;
        });

        // 2. 貸出先入力欄の挙動設定
        etBorrowerId.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                // 入力があれば、実行処理をトリガーする
                if (v.getText().length() > 0) {
                    executeLend();
                }
                return true;
            }
            return false;
        });

        // --- NFC初期化 ---
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        feliCaReader = new FeliCaReader();
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC機能がありません", Toast.LENGTH_SHORT).show();
        }

        // 初期フォーカスを備品欄へ
        etEquipmentCode.requestFocus();
    }

    // --- NFC ライフサイクル制御 ---
    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            Bundle options = new Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);
            nfcAdapter.enableReaderMode(
                    this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    options
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    // --- NFC 検知時 ---
    @Override
    public void onTagDiscovered(Tag tag) {
        feliCaReader.readStudentId(tag, new FeliCaReader.FeliCaCallback() {
            @Override
            public void onSuccess(@NonNull String studentId) {
                runOnUiThread(() -> {
                    etBorrowerId.setText(studentId);
                    Toast.makeText(LendingTerminalActivity.this, "学生証読取: " + studentId, Toast.LENGTH_SHORT).show();

                    // 備品番号が既に入力済みなら、そのまま実行してしまうのもアリ（運用次第）
                    // 今回は誤動作防止のためフォーカス移動のみ
                    etBorrowerId.requestFocus();
                    // カーソルを末尾へ
                    etBorrowerId.setSelection(etBorrowerId.getText().length());
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                runOnUiThread(() ->
                        Toast.makeText(LendingTerminalActivity.this, "読取失敗: " + exception.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // --- 貸出実行処理 ---
    private void executeLend() {
        // キーボードを閉じる（念の為）
        View view = this.getCurrentFocus();
        if (view != null) {
            // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            // imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        String managementNumber = etEquipmentCode.getText().toString().trim();
        String borrowerId = etBorrowerId.getText().toString().trim();

        // バリデーション
        if (managementNumber.isEmpty()) {
            Toast.makeText(this, "備品番号を入力してください", Toast.LENGTH_SHORT).show();
            etEquipmentCode.requestFocus();
            return;
        }
        if (borrowerId.isEmpty()) {
            Toast.makeText(this, "貸出先(学生証)を入力してください", Toast.LENGTH_SHORT).show();
            etBorrowerId.requestFocus();
            return;
        }

        // API設定取得
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");
        if (baseUrl.isEmpty()) {
            showResult(false, "API URL未設定");
            return;
        }
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // ユーザーID取得
        String currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) currentUserId = "unknown_terminal_user";

        // APIリクエスト作成
        int quantity = 1;
        CreateLendRequest requestBody = new CreateLendRequest(managementNumber, quantity, borrowerId, currentUserId);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        LendingApiService service = retrofit.create(LendingApiService.class);

        // ボタン連打防止
        btnExecute.setEnabled(false);
        btnExecute.setText("送信中...");

        service.createLend(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnExecute.setEnabled(true);
                btnExecute.setText("実行");

                if (response.isSuccessful()) {
                    showResult(true, "貸出登録 OK");
                    resetFields(); // 成功時のみ入力欄クリア
                } else {
                    showResult(false, "エラー: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnExecute.setEnabled(true);
                btnExecute.setText("実行");
                showResult(false, "通信エラー: " + t.getMessage());
            }
        });
    }

    // --- UIヘルパー ---

    // 結果表示 (OK/NG)
    private void showResult(boolean isSuccess, String message) {
        tvActionResult.setVisibility(View.VISIBLE);
        tvActionResult.setText(message);

        if (isSuccess) {
            tvActionResult.setTextColor(Color.WHITE);
            tvActionResult.setBackgroundColor(Color.parseColor("#4CAF50")); // 緑
        } else {
            tvActionResult.setTextColor(Color.WHITE);
            tvActionResult.setBackgroundColor(Color.parseColor("#F44336")); // 赤
        }
    }

    // 次の入力のためにリセット
    private void resetFields() {
        etEquipmentCode.setText("");
        // 運用によってはBorrowerIdを残す場合もあるが、基本はクリア
        etBorrowerId.setText("");

        // 備品番号入力欄にフォーカスを戻す
        etEquipmentCode.requestFocus();
    }
}