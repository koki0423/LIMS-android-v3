package com.example.lims_v3.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.ActiveLendResponse;
import com.example.lims_v3.network.CreateReturnRequest;
import com.example.lims_v3.network.ReturningApiService;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.google.zxing.ResultPoint;

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

    // API送信用に保持するフィールド
    private String currentLendUlid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);

        findViewById(R.id.btnBackReturn).setOnClickListener(v -> finish());

        // ライト制御
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

        // スキャナ設定
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner_return);
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if(result.getText() != null && !isModalShowing) {
                    isModalShowing = true;
                    barcodeScannerView.pause();
                    // スキャン結果(管理番号)を使って処理開始
                    fetchActiveLendAndShowModal(result.getText());
                }
            }
            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    // Step 1 & 2: 管理番号でAPIを叩き、成功したらモーダルを出す
    private void fetchActiveLendAndShowModal(String managementNumber) {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");

        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "API URL未設定", Toast.LENGTH_SHORT).show();
            barcodeScannerView.resume();
            isModalShowing = false;
            return;
        }
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ReturningApiService service = retrofit.create(ReturningApiService.class);

        // トースト等で読み込み中を通知しても良い
        // Toast.makeText(this, "貸出情報を検索中...", Toast.LENGTH_SHORT).show();

        service.getActiveLend(managementNumber).enqueue(new Callback<ActiveLendResponse>() {
            @Override
            public void onResponse(Call<ActiveLendResponse> call, Response<ActiveLendResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 成功：モーダルを表示
                    showReturnModal(response.body());
                } else {
                    // 失敗：貸出データが見つからない（既に返却済みなど）
                    Toast.makeText(ReturnActivity.this, "貸出中のデータが見つかりません", Toast.LENGTH_LONG).show();
                    barcodeScannerView.resume();
                    isModalShowing = false;
                }
            }

            @Override
            public void onFailure(Call<ActiveLendResponse> call, Throwable t) {
                Toast.makeText(ReturnActivity.this, "通信エラー: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                barcodeScannerView.resume();
                isModalShowing = false;
            }
        });
    }

    // Step 3: 取得したデータを使ってモーダル表示
    private void showReturnModal(ActiveLendResponse lendData) {
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

        // 取得したデータをセット
        currentLendUlid = lendData.getLendUlid(); // API送信用に保存
        etLendId.setText(lendData.getLendUlid()); // 画面表示
        etQuantity.setText(String.valueOf(lendData.getQuantity()));
        etBorrowerName.setText(lendData.getBorrowerId());

        // 日付は今日を自動入力 (表示用)
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etReturnDate.setText(today);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            cleanupState();
        });

        // Step 4: 返却実行
        btnExecReturn.setOnClickListener(v -> {
            executeReturn(lendData.getQuantity(), dialog);
        });

        dialog.show();
    }

    // Step 5: 返却API送信
    private void executeReturn(int defaultQuantity, AlertDialog dialog) {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // ログインユーザーID取得
        String currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) currentUserId = "unknown";

        // 数量は画面の入力値を優先（部分返却などの可能性を考慮する場合）
        // ただし基本はActiveLendから取得した値
        int quantity = defaultQuantity;
        try {
            // dialogからViewを探す必要がある場合は dialog.findViewById だが
            // ここではViewの参照を持っていないため、厳密にはEditText etQuantity を引数で渡すか、
            // Viewを保持しておく必要があります。
            // 今回は簡略化のため、APIから取得した quantity をそのまま使います。
            // 編集可能にするなら etQuantity.getText() をパースしてください。
        } catch (Exception e) {}

        // リクエスト作成
        CreateReturnRequest request = new CreateReturnRequest(quantity, currentLendUlid, currentUserId);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ReturningApiService service = retrofit.create(ReturningApiService.class);

        // ボタン連打防止等は省略していますが、入れるのがベター
        service.createReturn(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ReturnActivity.this, "返却完了！", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    cleanupState();
                } else {
                    Toast.makeText(ReturnActivity.this, "返却失敗: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ReturnActivity.this, "エラー: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cleanupState() {
        isModalShowing = false;
        currentLendUlid = null;
        barcodeScannerView.resume();
    }

    @Override
    protected void onResume() { super.onResume(); capture.onResume(); }
    @Override
    protected void onPause() { super.onPause(); capture.onPause(); }
    @Override
    protected void onDestroy() { super.onDestroy(); capture.onDestroy(); }
}