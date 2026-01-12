package com.example.lims_v3.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.util.FeliCaReader;
import com.example.lims_v3.network.AssetMasterResponse;
import com.example.lims_v3.network.CreateLendRequest;
import com.example.lims_v3.network.LendingApiService;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;

import java.util.List;

import com.google.zxing.ResultPoint;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LendingActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private boolean isModalShowing = false; // 二重読み取り防止フラグ

    // NFC用
    private NfcAdapter nfcAdapter;
    private FeliCaReader feliCaReader;

    // モーダル内の入力欄を操作するための参照
    private EditText currentBorrowerInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lending);

        // 戻るボタン
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ライトON/OFFボタン
        Button btnLight = findViewById(R.id.btnLight);
        btnLight.setOnClickListener(v -> {
            if (btnLight.getText().equals("ライトON")) {
                barcodeScannerView.setTorchOn(); // ライト点灯
                btnLight.setText("ライトOFF");
            } else {
                barcodeScannerView.setTorchOff(); // ライト消灯
                btnLight.setText("ライトON");
            }
        });

        // バーコードスキャナの設定
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        // 読み取りコールバック
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && !isModalShowing) {
                    isModalShowing = true;
                    barcodeScannerView.pause(); // 読み取り一時停止
                    showLendingModal(result.getText()); // モーダル表示
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
            }
        });

        // NFC初期化
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        feliCaReader = new FeliCaReader();
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC機能がありません", Toast.LENGTH_SHORT).show();
        }
    }

    // --- NFCライフサイクル制御 (QRと共存) ---
    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume(); // QR用

        // NFC用 ReaderMode有効化
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
        capture.onPause(); // QR用

        // NFC用 ReaderMode無効化
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    // --- ★ NFC検知時の処理 ---
    @Override
    public void onTagDiscovered(Tag tag) {
        // モーダルが開いていないときは無視しても良いし、メッセージを出しても良い
        if (!isModalShowing) {
            return;
        }

        feliCaReader.readStudentId(tag, new FeliCaReader.FeliCaCallback() {
            @Override
            public void onSuccess(@NonNull String studentId) {
                runOnUiThread(() -> {
                    // モーダルが開いていて、かつ入力欄の参照が生きていればセットする
                    if (isModalShowing && currentBorrowerInput != null) {
                        currentBorrowerInput.setText(studentId);
                        Toast.makeText(LendingActivity.this, "借受者を読み取りました: " + studentId, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(LendingActivity.this, "読み取り失敗: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // P7: モーダル（ダイアログ）の表示
    private void showLendingModal(String managementNumber) { // 引数名をわかりやすく managementNumber に変更
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_lending_confirm, null);
        builder.setView(dialogView);

        TextView tvEquipmentId = dialogView.findViewById(R.id.tvEquipmentId);
        TextView tvEquipmentName = dialogView.findViewById(R.id.tvEquipmentName);
        TextView tvStock = dialogView.findViewById(R.id.tvStock);
        EditText etBorrower = dialogView.findViewById(R.id.etBorrower);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnLend = dialogView.findViewById(R.id.btnLend);

        // スキャンした管理番号を表示
        tvEquipmentId.setText(managementNumber);
        tvEquipmentName.setText("情報を取得中...");
        tvStock.setText("1");

        currentBorrowerInput = etBorrower;

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        fetchAssetInfo(managementNumber, tvEquipmentName);

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            cleanupModalState();
        });

        // ★ 貸出ボタン押下時のAPI処理 ★
        btnLend.setOnClickListener(v -> {
            String borrowerId = etBorrower.getText().toString();
            if (borrowerId.isEmpty()) {
                Toast.makeText(this, "借受者を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. 設定からAPIのベースURLを取得
            SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
            String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");

            if (baseUrl.isEmpty()) {
                Toast.makeText(this, "設定画面でAPI URLを設定してください", Toast.LENGTH_LONG).show();
                return;
            }

            // Retrofitの末尾スラッシュ補完（安全策）
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            // 2. ログインユーザーIDの取得 (MenuActivityからIntentで渡されている想定)
            String currentUserId = getIntent().getStringExtra("USER_ID");
            if (currentUserId == null) currentUserId = "unknown_user"; // フォールバック

            // 3. リクエストデータの作成
            // 数量は画面設計書P7の「在庫数量：1」に従い固定または入力値を使用
            int quantity = 1;
            CreateLendRequest requestBody = new CreateLendRequest(quantity, borrowerId, currentUserId);

            // 4. Retrofitインスタンスの生成
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            LendingApiService service = retrofit.create(LendingApiService.class);

            // 5. APIコール実行
            Call<Void> call = service.createLend(managementNumber, requestBody);

            // ボタンを連打できないように無効化したりプログレスバーを出すのがベター
            btnLend.setEnabled(false);
            btnLend.setText("送信中...");

            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(LendingActivity.this, "貸出登録完了！", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                         cleanupModalState();
                    } else {
                        // エラーハンドリング (400 Bad Request, 500 Internal Server Error など)
                        Toast.makeText(LendingActivity.this, "登録失敗: " + response.code(), Toast.LENGTH_SHORT).show();
                        btnLend.setEnabled(true);
                        btnLend.setText("貸出");
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(LendingActivity.this, "通信エラー: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    btnLend.setEnabled(true);
                    btnLend.setText("貸出");
                }
            });
        });

        dialog.show();
    }

    // モーダルを閉じるときのリセット処理
    private void cleanupModalState() {
        isModalShowing = false;
        currentBorrowerInput = null; // 参照を切る
        barcodeScannerView.resume();
    }

    // マスタ情報を取得してTextViewを更新するメソッド
    private void fetchAssetInfo(String managementNumber, TextView targetView) {
        // 1. 設定からBase URL取得
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");

        if (baseUrl.isEmpty()) {
            targetView.setText("URL設定エラー");
            return;
        }
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        // 2. Retrofit生成
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        LendingApiService service = retrofit.create(LendingApiService.class);

        // 3. GETリクエスト実行
        service.getAssetMaster(managementNumber).enqueue(new Callback<AssetMasterResponse>() {
            @Override
            public void onResponse(Call<AssetMasterResponse> call, Response<AssetMasterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 成功したら備品名をセット
                    targetView.setText(response.body().getName());
                } else {
                    targetView.setText("備品情報が見つかりません");
                }
            }

            @Override
            public void onFailure(Call<AssetMasterResponse> call, Throwable t) {
                targetView.setText("通信エラー");
            }
        });
    }
}