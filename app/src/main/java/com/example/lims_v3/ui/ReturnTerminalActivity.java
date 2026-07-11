package com.example.lims_v3.ui;

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
import com.example.lims_v3.util.ApiClientFactory;
import com.example.lims_v3.util.ApiConfig;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReturnTerminalActivity extends AppCompatActivity {

    private EditText etEquipmentCode;
    private TextView tvActionResult;
    private Button btnExecute;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_terminal);

        etEquipmentCode = findViewById(R.id.etEquipmentCode);
        tvActionResult = findViewById(R.id.tvActionResult);
        btnExecute = findViewById(R.id.btnExecute);
        btnBack = findViewById(R.id.btnBack);

        tvActionResult.setVisibility(View.GONE);

        btnBack.setOnClickListener(v -> finish());
        btnExecute.setOnClickListener(v -> executeReturnProcess());

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        etEquipmentCode.setShowSoftInputOnFocus(false);

        etEquipmentCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (v.getText().length() > 0) {
                    executeReturnProcess();
                }
                return true;
            }
            return false;
        });

        etEquipmentCode.requestFocus();
    }

    private void executeReturnProcess() {
        String managementNumber = etEquipmentCode.getText().toString().trim();

        if (managementNumber.isEmpty()) {
            Toast.makeText(this, "備品番号をスキャンしてください", Toast.LENGTH_SHORT).show();
            etEquipmentCode.requestFocus();
            return;
        }

        String baseUrl = getBaseUrl();
        if (baseUrl.isEmpty()) {
            showResult(false, "API URL未設定");
            return;
        }

        ReturningApiService service = buildReturningService(baseUrl);

        btnExecute.setEnabled(false);
        btnExecute.setText("処理中...");

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

                performReturn(service, items.get(0));
            }

            @Override
            public void onFailure(Call<List<LendResponse>> call, Throwable t) {
                handleError("通信エラー(検索): " + t.getMessage());
            }
        });
    }

    private void performReturn(ReturningApiService service, LendResponse lendData) {
        String currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) {
            currentUserId = "unknown_terminal";
        }

        CreateReturnRequest request = new CreateReturnRequest(lendData.getQuantity(), currentUserId);

        service.createReturn(lendData.getLendUlid(), request).enqueue(new Callback<Void>() {
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

    private void handleError(String msg) {
        showResult(false, msg);
        btnExecute.setEnabled(true);
        btnExecute.setText("実行");
        etEquipmentCode.requestFocus();
        etEquipmentCode.selectAll();
    }

    private void showResult(boolean isSuccess, String message) {
        tvActionResult.setVisibility(View.VISIBLE);
        tvActionResult.setText(message);

        if (isSuccess) {
            tvActionResult.setTextColor(Color.WHITE);
            tvActionResult.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else {
            tvActionResult.setTextColor(Color.WHITE);
            tvActionResult.setBackgroundColor(Color.parseColor("#F44336"));
        }
    }

    private void resetForNext() {
        btnExecute.setEnabled(true);
        btnExecute.setText("実行");
        etEquipmentCode.setText("");
        etEquipmentCode.requestFocus();
    }

    private String getBaseUrl() {
        try {
            return ApiConfig.requireBaseUrl(this);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return "";
        }
    }

    private ReturningApiService buildReturningService(String baseUrl) {
        return ApiClientFactory.createService(this, baseUrl, ReturningApiService.class);
    }
}
