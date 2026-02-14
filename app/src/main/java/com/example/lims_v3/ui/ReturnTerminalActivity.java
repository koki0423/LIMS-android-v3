package com.example.lims_v3.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.CreateReturnRequest;
import com.example.lims_v3.network.LendResponse;
import com.example.lims_v3.network.ReturningApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ReturnTerminalActivity extends AppCompatActivity {

    // UIコンポーネント
    private EditText etEquipmentCode;
    private TextView tvActionResult;
    private Button btnExecute;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_terminal);

        // --- UI紐付け ---
        etEquipmentCode = findViewById(R.id.etEquipmentCode);
        tvActionResult = findViewById(R.id.tvActionResult);
        btnExecute = findViewById(R.id.btnExecute);
        btnBack = findViewById(R.id.btnBack);

        // 初期表示
        tvActionResult.setVisibility(View.GONE);

        // 戻るボタン
        btnBack.setOnClickListener(v -> finish());

        // 実行ボタン (手動用)
        btnExecute.setOnClickListener(v -> executeReturnProcess());

        // --- ハンディ端末向け設定 ---
        // 起動時にソフトキーボードを出さない
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        etEquipmentCode.setShowSoftInputOnFocus(false);

        // スキャナ入力イベント設定 (Enterキーで実行)
        etEquipmentCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                // 入力があれば処理開始
                if (v.getText().length() > 0) {
                    executeReturnProcess();
                }
                return true; // イベント消費
            }
            return false;
        });

        // 初期フォーカス
        etEquipmentCode.requestFocus();
    }

    /**
     * 返却処理メインフロー
     * 1. 入力チェック
     * 2. 貸出中データ検索 (Search Active Lend)
     * 3. 返却実行 (Create Return)
     */
    private void executeReturnProcess() {
        // キーボードを閉じる（念の為）
        View view = this.getCurrentFocus();
        if (view != null) {
            // InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            // imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        String managementNumber = etEquipmentCode.getText().toString().trim();

        if (managementNumber.isEmpty()) {
            Toast.makeText(this, "備品番号をスキャンしてください", Toast.LENGTH_SHORT).show();
            etEquipmentCode.requestFocus();
            return;
        }

        // URL取得
        String baseUrl = getBaseUrl();
        if (baseUrl.isEmpty()) {
            showResult(false, "API URL未設定");
            return;
        }

        // Service作成
        ReturningApiService service = buildReturningService(baseUrl);

        // ボタン制御
        btnExecute.setEnabled(false);
        btnExecute.setText("処理中...");

        // --- Step 1: 貸出中検索 ---
        service.searchActiveLend(managementNumber, true).enqueue(new Callback<List<LendResponse>>() {
            @Override
            public void onResponse(Call<List<LendResponse>> call, Response<List<LendResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    handleError("検索失敗: " + response.code());
                    return;
                }

                List<LendResponse> items = response.body();
                if (items.isEmpty()) {
                    handleError("貸出中のデータ無し");
                    return;
                }

                // 対象データ取得 (リストの先頭)
                LendResponse targetLend = items.get(0);
                performReturn(service, targetLend);
            }

            @Override
            public void onFailure(Call<List<LendResponse>> call, Throwable t) {
                handleError("通信エラー(検索): " + t.getMessage());
            }
        });
    }

    /**
     * Step 2: 返却実行 APIコール
     */
    private void performReturn(ReturningApiService service, LendResponse lendData) {
        String lendUlid = lendData.getLendUlid();
        int quantity = lendData.getQuantity(); // 全数返却とする

        // ユーザーID
        String currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) currentUserId = "unknown_terminal";

        CreateReturnRequest request = new CreateReturnRequest(quantity, currentUserId);

        service.createReturn(lendUlid, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showResult(true, "返却完了: " + lendData.getManagementNumber());
                    resetForNext();
                } else {
                    handleError("返却失敗: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                handleError("通信エラー(返却): " + t.getMessage());
            }
        });
    }

    // --- ヘルパーメソッド ---

    private void handleError(String msg) {
        showResult(false, msg);
        btnExecute.setEnabled(true);
        btnExecute.setText("実行");
        // エラー時もフォーカスは戻してあげる（再試行しやすくする）
        etEquipmentCode.requestFocus();
        etEquipmentCode.selectAll();
    }

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

    private void resetForNext() {
        btnExecute.setEnabled(true);
        btnExecute.setText("実行");

        // 入力クリア
        etEquipmentCode.setText("");
        etEquipmentCode.requestFocus();
    }

    private String getBaseUrl() {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");
        if (baseUrl == null) return "";
        baseUrl = baseUrl.trim();
        if (baseUrl.isEmpty()) return "";
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        return baseUrl;
    }

    private ReturningApiService buildReturningService(String baseUrl) {
        // 日付フォーマット対応
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(ReturningApiService.class);
    }
}