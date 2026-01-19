package com.example.lims_v3.ui;

import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.CreateReturnRequest;
import com.example.lims_v3.network.LendResponse;
import com.example.lims_v3.network.ListLendsResult;
import com.example.lims_v3.network.ReturningApiService;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.google.zxing.ResultPoint;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ReturnActivity extends AppCompatActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private boolean isModalShowing = false;

    // 返却実行時に必要なIDを保持
    private String currentLendUlid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);

        findViewById(R.id.btnBackReturn).setOnClickListener(v -> finish());

        // ライトボタン設定（省略せずに記述）
        Button btnLight = findViewById(R.id.btnLightReturn);
        btnLight.setOnClickListener(v -> {
            if (btnLight.getText().equals("ライトON")) {
                barcodeScannerView.setTorchOn();
                btnLight.setText("ライトOFF");
            } else {
                barcodeScannerView.setTorchOff();
                btnLight.setText("ライトON");
            }
        });

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner_return);
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && !isModalShowing) {
                    isModalShowing = true;
                    barcodeScannerView.pause();
                    // スキャンした管理番号で検索開始
                    searchActiveLend(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
            }
        });
    }

    // Step 1: 管理番号から貸出中のデータを検索
    private void searchActiveLend(String managementNumber) {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");

        if (baseUrl.isEmpty()) {
            showToast("API URL未設定");
            cleanupState();
            return;
        }
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // Gsonの設定を作成
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss") // Goの標準フォーマットに合わせる
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        ReturningApiService service = retrofit.create(ReturningApiService.class);

        // only_outstanding=true で検索
        service.searchActiveLend(managementNumber, true).enqueue(new Callback<List<LendResponse>>() {
            @Override
            public void onResponse(Call<List<LendResponse>> call, Response<List<LendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LendResponse> items = response.body(); // 直接リストを取得
                    if (!items.isEmpty()) {
                        showReturnModal(items.get(0));
                    } else {
                        showToast("貸出中のデータが見つかりません");
                        cleanupState();
                    }
                } else {
                    showToast("検索失敗: " + response.code());
                    cleanupState();
                }
            }

            @Override
            public void onFailure(Call<List<LendResponse>> call, Throwable t) {
                Toast.makeText(ReturnActivity.this, "通信エラー: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Step 2: モーダル表示
    private void showReturnModal(LendResponse lendData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_return_confirm, null);
        builder.setView(dialogView);

        EditText etLendId = dialogView.findViewById(R.id.etLendId);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etBorrowerName = dialogView.findViewById(R.id.etBorrowerName);
        EditText etReturnDate = dialogView.findViewById(R.id.etReturnDate);
        Button btnClose = dialogView.findViewById(R.id.btnCloseReturn);
        Button btnExecReturn = dialogView.findViewById(R.id.btnExecReturn);

        // データのセット
        currentLendUlid = lendData.getLendUlid(); // API送信用にULIDを保存
        // 画面上には管理番号を表示した方が分かりやすいが、レイアウト定義に従いULIDを表示するか、
        // あるいは etLendId に管理番号(lendData.getManagementNumber())を入れるかは運用次第
        etLendId.setText(lendData.getManagementNumber());

        etQuantity.setText(String.valueOf(lendData.getQuantity()));
        etBorrowerName.setText(lendData.getBorrowerId());

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etReturnDate.setText(today);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            cleanupState();
        });

        // Step 3: 返却実行
        btnExecReturn.setOnClickListener(v -> {
            // 入力された数量を取得（部分返却対応の場合）
            int returnQty = lendData.getQuantity(); // デフォルトは全返却
            try {
                returnQty = Integer.parseInt(etQuantity.getText().toString());
            } catch (NumberFormatException e) {
            }

            executeReturn(currentLendUlid, returnQty, dialog);
        });

        dialog.show();
    }

    // Step 4: API送信
    private void executeReturn(String lendUlid, int quantity, AlertDialog dialog) {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        String currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) currentUserId = "unknown";

        // CreateReturnRequestには lend_ulid は不要になった
        CreateReturnRequest request = new CreateReturnRequest(quantity, currentUserId);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ReturningApiService service = retrofit.create(ReturningApiService.class);

        service.createReturn(lendUlid,request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showToast("返却完了！");
                    dialog.dismiss();
                    cleanupState();
                } else {
                    showToast("返却失敗: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showToast("エラー: " + t.getMessage());
            }
        });
    }

    private void cleanupState() {
        isModalShowing = false;
        currentLendUlid = null;
        barcodeScannerView.resume();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }
}