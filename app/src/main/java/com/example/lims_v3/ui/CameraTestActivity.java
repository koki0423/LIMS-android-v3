package com.example.lims_v3.ui;

import android.Manifest;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.util.CameraPermissionHelper;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class CameraTestActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1003;

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private Button btnLight;
    private boolean scannerInitialized = false;
    private Bundle initialSavedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_test);
        initialSavedInstanceState = savedInstanceState;

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner_test);

        findViewById(R.id.btnBackCameraTest).setOnClickListener(v -> finish());

        btnLight = findViewById(R.id.btnLightTest);
        btnLight.setOnClickListener(v -> {
            if ("ライトON".equals(btnLight.getText())) {
                barcodeScannerView.setTorchOn();
                btnLight.setText("ライトOFF");
            } else {
                barcodeScannerView.setTorchOff();
                btnLight.setText("ライトON");
            }
        });
        btnLight.setEnabled(false);

        ensureCameraPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (capture != null) {
            capture.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    private void ensureCameraPermission() {
        if (CameraPermissionHelper.hasCameraPermission(this)) {
            initializeScanner();
            return;
        }
        CameraPermissionHelper.requestCameraPermission(this, CAMERA_PERMISSION_REQUEST_CODE);
    }

    private void initializeScanner() {
        if (scannerInitialized) {
            return;
        }

        capture = new CaptureManager(this, barcodeScannerView);
        capture.initializeFromIntent(getIntent(), initialSavedInstanceState);
        capture.decode();
        barcodeScannerView.decodeContinuous(result -> {
            if (result.getText() != null) {
                barcodeScannerView.pause();
                new AlertDialog.Builder(this)
                        .setTitle("読み取り成功")
                        .setMessage("内容: " + result.getText())
                        .setPositiveButton("OK", (dialog, which) -> barcodeScannerView.resume())
                        .setCancelable(false)
                        .show();
            }
        });
        scannerInitialized = true;
        btnLight.setEnabled(true);
        capture.onResume();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CAMERA_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (permissions.length > 0
                && Manifest.permission.CAMERA.equals(permissions[0])
                && CameraPermissionHelper.isPermissionGranted(grantResults)) {
            initializeScanner();
            return;
        }

        Toast.makeText(this, "カメラ権限が必要です", Toast.LENGTH_LONG).show();
        finish();
    }
}
