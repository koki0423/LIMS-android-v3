package com.example.lims_v3.ui;

import android.Manifest;
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
import com.example.lims_v3.network.AssetMasterResponse;
import com.example.lims_v3.network.CreateLendRequest;
import com.example.lims_v3.network.LendingApiService;
import com.example.lims_v3.util.ApiClientFactory;
import com.example.lims_v3.util.CameraPermissionHelper;
import com.example.lims_v3.util.FeliCaReader;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LendingActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private Button btnLight;
    private boolean isModalShowing = false;
    private boolean scannerInitialized = false;
    private Bundle initialSavedInstanceState;
    private NfcAdapter nfcAdapter;
    private FeliCaReader feliCaReader;
    private EditText currentBorrowerInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lending);
        initialSavedInstanceState = savedInstanceState;

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnLight = findViewById(R.id.btnLight);
        btnLight.setOnClickListener(v -> {
            if ("ライトON".contentEquals(btnLight.getText())) {
                barcodeScannerView.setTorchOn();
                btnLight.setText("ライトOFF");
            } else {
                barcodeScannerView.setTorchOff();
                btnLight.setText("ライトON");
            }
        });
        btnLight.setEnabled(false);

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        feliCaReader = new FeliCaReader();
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC機能がありません", Toast.LENGTH_SHORT).show();
        }

        ensureCameraPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (capture != null) {
            capture.onResume();
        }

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
        if (capture != null) {
            capture.onPause();
        }

        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (capture != null) {
            capture.onDestroy();
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        if (!isModalShowing) {
            return;
        }

        feliCaReader.readStudentId(tag, new FeliCaReader.FeliCaCallback() {
            @Override
            public void onSuccess(@NonNull String studentId) {
                runOnUiThread(() -> {
                    if (isModalShowing && currentBorrowerInput != null) {
                        currentBorrowerInput.setText(studentId);
                        Toast.makeText(LendingActivity.this, "借受者を読み取りました: " + studentId, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                runOnUiThread(() ->
                        Toast.makeText(LendingActivity.this, "読み取り失敗: " + exception.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showLendingModal(String managementNumber) {
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

        btnLend.setOnClickListener(v -> {
            String borrowerId = etBorrower.getText().toString().trim();
            if (borrowerId.isEmpty()) {
                Toast.makeText(this, "借受者を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            final LendingApiService service;
            try {
                service = ApiClientFactory.createService(this, LendingApiService.class);
            } catch (IllegalStateException | IllegalArgumentException exception) {
                Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            String currentUserId = getIntent().getStringExtra("USER_ID");
            if (currentUserId == null) {
                currentUserId = "unknown_user";
            }

            CreateLendRequest requestBody = new CreateLendRequest(managementNumber, 1, borrowerId, currentUserId);
            btnLend.setEnabled(false);
            btnLend.setText("送信中...");

            service.createLend(requestBody).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(LendingActivity.this, "貸出登録完了！", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cleanupModalState();
                    } else {
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

    private void cleanupModalState() {
        isModalShowing = false;
        currentBorrowerInput = null;
        if (barcodeScannerView != null) {
            barcodeScannerView.resume();
        }
    }

    private void fetchAssetInfo(String managementNumber, TextView targetView) {
        final LendingApiService service;
        try {
            service = ApiClientFactory.createService(this, LendingApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            targetView.setText(exception.getMessage());
            return;
        }

        service.getAssetMaster(managementNumber).enqueue(new Callback<AssetMasterResponse>() {
            @Override
            public void onResponse(Call<AssetMasterResponse> call, Response<AssetMasterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
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
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && !isModalShowing) {
                    isModalShowing = true;
                    barcodeScannerView.pause();
                    showLendingModal(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
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
