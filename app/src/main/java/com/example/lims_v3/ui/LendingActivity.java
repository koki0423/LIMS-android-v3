package com.example.lims_v3.ui;

import android.content.DialogInterface;
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
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import java.util.List;
import com.google.zxing.ResultPoint;

public class LendingActivity extends AppCompatActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private boolean isModalShowing = false; // 二重読み取り防止フラグ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lending);

        // 戻るボタン
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ライトON/OFFボタン [cite: 38]
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

        // バーコードスキャナの設定 [cite: 35]
        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);
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
                    showLendingModal(result.getText()); // モーダル表示 [cite: 37]
                }
            }
            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });
    }

    // P7: モーダル（ダイアログ）の表示
    private void showLendingModal(String equipmentId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_lending_confirm, null);
        builder.setView(dialogView);

        // UI要素の紐付け
        TextView tvEquipmentId = dialogView.findViewById(R.id.tvEquipmentId);
        TextView tvEquipmentName = dialogView.findViewById(R.id.tvEquipmentName);
        TextView tvStock = dialogView.findViewById(R.id.tvStock);
        EditText etBorrower = dialogView.findViewById(R.id.etBorrower);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        Button btnLend = dialogView.findViewById(R.id.btnLend);

        // データのセット (本来はAPI等から取得) [cite: 44, 46, 48]
        tvEquipmentId.setText(equipmentId);
        tvEquipmentName.setText("Arduinoキット (仮)");
        tvStock.setText("1");

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // 枠外タップで閉じない

        // 閉じるボタン [cite: 51]
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            isModalShowing = false;
            barcodeScannerView.resume(); // 読み取り再開
        });

        // 貸出ボタン [cite: 53]
        btnLend.setOnClickListener(v -> {
            String borrower = etBorrower.getText().toString();
            // TODO: 貸出APIを叩く処理
            Toast.makeText(this, "貸出処理完了: " + borrower, Toast.LENGTH_SHORT).show();
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