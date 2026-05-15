package com.example.lims_v3.ui;

import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.CreateLendRequest;
import com.example.lims_v3.network.LendingApiService;
import com.example.lims_v3.util.ApiClientFactory;
import com.example.lims_v3.util.FeliCaReader;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LendingTerminalActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private EditText etEquipmentCode;
    private EditText etBorrowerId;
    private TextView tvActionResult;
    private Button btnExecute;
    private NfcAdapter nfcAdapter;
    private FeliCaReader feliCaReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lending_terminal);

        etEquipmentCode = findViewById(R.id.etEquipmentCode);
        etBorrowerId = findViewById(R.id.etBorrowerId);
        tvActionResult = findViewById(R.id.tvActionResult);
        btnExecute = findViewById(R.id.btnExecute);

        tvActionResult.setVisibility(View.GONE);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnExecute.setOnClickListener(v -> executeLend());

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        etEquipmentCode.setShowSoftInputOnFocus(false);
        etBorrowerId.setShowSoftInputOnFocus(false);

        etEquipmentCode.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (v.getText().length() > 0) {
                    etBorrowerId.requestFocus();
                }
                return true;
            }
            return false;
        });

        etBorrowerId.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                if (v.getText().length() > 0) {
                    executeLend();
                }
                return true;
            }
            return false;
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        feliCaReader = new FeliCaReader();
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC機能がありません", Toast.LENGTH_SHORT).show();
        }

        etEquipmentCode.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        feliCaReader.readStudentId(tag, new FeliCaReader.FeliCaCallback() {
            @Override
            public void onSuccess(@NonNull String studentId) {
                runOnUiThread(() -> {
                    etBorrowerId.setText(studentId);
                    Toast.makeText(LendingTerminalActivity.this, "学生証読取: " + studentId, Toast.LENGTH_SHORT).show();
                    etBorrowerId.requestFocus();
                    etBorrowerId.setSelection(etBorrowerId.getText().length());
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                runOnUiThread(() ->
                        Toast.makeText(LendingTerminalActivity.this, "読取失敗: " + exception.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void executeLend() {
        String managementNumber = etEquipmentCode.getText().toString().trim();
        String borrowerId = etBorrowerId.getText().toString().trim();

        if (managementNumber.isEmpty()) {
            Toast.makeText(this, "備品番号を入力してください", Toast.LENGTH_SHORT).show();
            etEquipmentCode.requestFocus();
            return;
        }
        if (borrowerId.isEmpty()) {
            Toast.makeText(this, "貸出先(学生証)を入力してください", Toast.LENGTH_SHORT).show();
            etBorrowerId.requestFocus();
            return;
        }

        final LendingApiService service;
        try {
            service = ApiClientFactory.createService(this, LendingApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            showResult(false, exception.getMessage());
            return;
        }

        String currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) {
            currentUserId = "unknown_terminal_user";
        }

        CreateLendRequest requestBody = new CreateLendRequest(managementNumber, 1, borrowerId, currentUserId);

        btnExecute.setEnabled(false);
        btnExecute.setText("送信中...");

        service.createLend(requestBody).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnExecute.setEnabled(true);
                btnExecute.setText("実行");

                if (response.isSuccessful()) {
                    showResult(true, "貸出登録 OK");
                    resetFields();
                } else {
                    showResult(false, "エラー: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnExecute.setEnabled(true);
                btnExecute.setText("実行");
                showResult(false, "通信エラー: " + t.getMessage());
            }
        });
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

    private void resetFields() {
        etEquipmentCode.setText("");
        etBorrowerId.setText("");
        etEquipmentCode.requestFocus();
    }
}
