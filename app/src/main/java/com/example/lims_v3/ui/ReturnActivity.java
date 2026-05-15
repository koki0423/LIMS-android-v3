package com.example.lims_v3.ui;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.CreateReturnRequest;
import com.example.lims_v3.network.LendResponse;
import com.example.lims_v3.network.ReturningApiService;
import com.example.lims_v3.util.ApiClientFactory;
import com.example.lims_v3.util.CameraPermissionHelper;
import com.example.lims_v3.util.ReturnInputValidator;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReturnActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1002;

    private CaptureManager capture;
    private DecoratedBarcodeView barcodeScannerView;
    private Button btnLight;
    private boolean isModalShowing = false;
    private boolean scannerInitialized = false;
    private Bundle initialSavedInstanceState;
    private String currentLendUlid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return);
        initialSavedInstanceState = savedInstanceState;

        findViewById(R.id.btnBackReturn).setOnClickListener(v -> finish());

        btnLight = findViewById(R.id.btnLightReturn);
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

        barcodeScannerView = findViewById(R.id.zxing_barcode_scanner_return);
        ensureCameraPermission();
    }

    private void searchActiveLend(String managementNumber) {
        final ReturningApiService service;
        try {
            service = ApiClientFactory.createService(this, ReturningApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            showToast(exception.getMessage());
            cleanupState();
            return;
        }

        service.searchActiveLend(managementNumber, true).enqueue(new Callback<List<LendResponse>>() {
            @Override
            public void onResponse(Call<List<LendResponse>> call, Response<List<LendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<LendResponse> items = response.body();
                    if (!items.isEmpty()) {
                        showReturnModal(items.get(0), service);
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
                cleanupState();
            }
        });
    }

    private void showReturnModal(LendResponse lendData, ReturningApiService service) {
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

        currentLendUlid = lendData.getLendUlid();
        etLendId.setText(lendData.getManagementNumber());
        etQuantity.setText(String.valueOf(lendData.getQuantity()));
        etBorrowerName.setText(lendData.getBorrowerId());
        etReturnDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            cleanupState();
        });

        btnExecReturn.setOnClickListener(v -> {
            try {
                int returnQty = ReturnInputValidator.parseReturnQuantity(etQuantity.getText().toString(), lendData.getQuantity());
                setReturnSubmissionState(btnExecReturn, btnClose, true);
                executeReturn(service, currentLendUlid, returnQty, dialog, btnExecReturn, btnClose);
            } catch (IllegalArgumentException exception) {
                showToast(exception.getMessage());
            }
        });

        dialog.show();
    }

    private void executeReturn(
            ReturningApiService service,
            String lendUlid,
            int quantity,
            AlertDialog dialog,
            Button btnExecReturn,
            Button btnClose
    ) {
        String currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) {
            currentUserId = "unknown";
        }

        CreateReturnRequest request = new CreateReturnRequest(quantity, currentUserId);
        service.createReturn(lendUlid, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showToast("返却完了！");
                    dialog.dismiss();
                    cleanupState();
                } else {
                    showToast("返却失敗: " + response.code());
                    setReturnSubmissionState(btnExecReturn, btnClose, false);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showToast("エラー: " + t.getMessage());
                setReturnSubmissionState(btnExecReturn, btnClose, false);
            }
        });
    }

    private void cleanupState() {
        isModalShowing = false;
        currentLendUlid = null;
        if (barcodeScannerView != null) {
            barcodeScannerView.resume();
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        barcodeScannerView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && !isModalShowing) {
                    isModalShowing = true;
                    barcodeScannerView.pause();
                    searchActiveLend(result.getText());
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

    private void setReturnSubmissionState(Button btnExecReturn, Button btnClose, boolean isSubmitting) {
        btnExecReturn.setEnabled(!isSubmitting);
        btnClose.setEnabled(!isSubmitting);
        btnExecReturn.setText(isSubmitting ? "送信中..." : "返却実行");
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
