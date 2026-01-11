package com.example.lims_v3.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReturnActivity extends AppCompatActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private boolean isModalShowing = false; // 二重読み取り防止フラグ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);

        // 戻るボタン
        findViewById(R.id.btnBackReturn).setOnClickListener(v -> finish());

        // ライトON/OFFボタン [cite: 62]
        Button btnLight = findViewById(R.id.btnLightReturn);
        btnLight.setOnClickListener(v -> {
            if (btnLight.getText().equals("ライトON")) {
                barcodeScannerView.setTorchOn(); // ライト点灯
                btnLight.setText("ライトOFF");
            } else {
                barcodeScannerView.setTorchOff(); // ライト消灯
                btnLight.setText("ライトON");
            }
        });

        // バーコードスキャナの設定 [cite: 59]
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner_return);
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        // 読み取りコールバック
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if(result.getText() != null && !isModalShowing) {
                    isModalShowing = true;
                    barcodeScannerView.pause(); // 読み取り一時停止
                    showReturnModal(result.getText()); // 返却モーダル表示 [cite: 61]
                }
            }
            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    // P9: 返却登録モーダルの表示
    private void showReturnModal(String scannedCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        // 返却用のレイアウトをインフレート [cite: 66]
        View dialogView = inflater.inflate(R.layout.dialog_return_confirm, null);
        builder.setView(dialogView);

        // UI要素の紐付け (dialog_return_confirm.xmlのIDに合わせる)
        EditText etLendId = dialogView.findViewById(R.id.etLendId);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etBorrowerName = dialogView.findViewById(R.id.etBorrowerName);
        EditText etReturnDate = dialogView.findViewById(R.id.etReturnDate);
        Button btnClose = dialogView.findViewById(R.id.btnCloseReturn);
        Button btnExecReturn = dialogView.findViewById(R.id.btnExecReturn);

       // データのセット (モック用データ) [cite: 67, 69, 71]
        // 本来はscannedCodeを使ってDBから貸出情報を検索・取得する
        etLendId.setText(scannedCode); // 読み取ったコードを貸出番号として表示
        etQuantity.setText("1");
        etBorrowerName.setText("AB12345");

        // 返却日 (自動入力: 本日の日付) [cite: 73]
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etReturnDate.setText(today);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // 枠外タップで閉じない

       // 閉じるボタン [cite: 75]
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            isModalShowing = false;
            barcodeScannerView.resume(); // 読み取り再開
        });

        // 返却実行ボタン [cite: 77]
        btnExecReturn.setOnClickListener(v -> {
            // TODO: 返却APIを叩く処理
            Toast.makeText(this, "返却処理完了: " + scannedCode, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            isModalShowing = false;
            barcodeScannerView.resume(); // 続けて読み取り
        });

        dialog.show();
    }

    @Override
    protected void onResume() { super.onResume(); capture.onResume(); }
    @Override
    protected void onPause() { super.onPause(); capture.onPause(); }
    @Override
    protected void onDestroy() { super.onDestroy(); capture.onDestroy(); }
}