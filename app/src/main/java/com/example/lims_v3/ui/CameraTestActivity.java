package com.example.lims_v3.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lims_v3.R;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CameraTestActivity extends AppCompatActivity {

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_test); // activity_return.xmlのコピーなどでOK

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner_test);

        // 戻るボタン
        findViewById(R.id.btnBackCameraTest).setOnClickListener(v -> finish());

        // ライトボタン
        Button btnLight = findViewById(R.id.btnLightTest);
        btnLight.setOnClickListener(v -> {
            if("ライトON".equals(btnLight.getText())) {
                barcodeScannerView.setTorchOn();
                btnLight.setText("ライトOFF");
            } else {
                barcodeScannerView.setTorchOff();
                btnLight.setText("ライトON");
            }
        });

        // スキャナ設定
        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        // 読み取り時のコールバック
        barcodeScannerView.decodeContinuous(result -> {
            if(result.getText() != null) {
                barcodeScannerView.pause(); // 連続読み取り防止

                // 結果をダイアログ表示
                new AlertDialog.Builder(this)
                        .setTitle("読み取り成功")
                        .setMessage("内容: " + result.getText())
                        .setPositiveButton("OK", (dialog, which) -> {
                            barcodeScannerView.resume(); // スキャン再開
                        })
                        .setCancelable(false)
                        .show();
            }
        });
    }

    @Override
    protected void onResume() { super.onResume(); capture.onResume(); }
    @Override
    protected void onPause() { super.onPause(); capture.onPause(); }
    @Override
    protected void onDestroy() { super.onDestroy(); capture.onDestroy(); }
}