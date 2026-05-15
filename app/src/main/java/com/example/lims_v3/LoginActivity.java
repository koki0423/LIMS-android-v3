package com.example.lims_v3;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.network.AuthApiService;
import com.example.lims_v3.network.LoginRequest;
import com.example.lims_v3.network.TokenResponse;
import com.example.lims_v3.ui.MenuActivity;
import com.example.lims_v3.util.ApiClientFactory;
import com.example.lims_v3.util.AuthSessionManager;
import com.example.lims_v3.util.FeliCaReader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    private EditText etUserId;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnStudentCardAuth;
    private NfcAdapter nfcAdapter;
    private FeliCaReader feliCaReader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // UI初期化
        etUserId = findViewById(R.id.etUserId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnStudentCardAuth = findViewById(R.id.btnStudentCardAuth);

        // NFCアダプターとリーダーの初期化
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        feliCaReader = new FeliCaReader();

        if (nfcAdapter == null) {
            Toast.makeText(this, "このデバイスはNFCに対応していません", Toast.LENGTH_LONG).show();
            btnStudentCardAuth.setEnabled(false);
        }

        // 通常ログインボタン
        btnLogin.setOnClickListener(v -> attemptPasswordLogin());

        // 学生証認証ボタン
        // NFC ReaderModeはonResumeで有効になるため、ボタンは「ガイド表示」として使います
        btnStudentCardAuth.setOnClickListener(v -> {
            if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                Toast.makeText(this, "学生証を端末の背面にタッチしてください", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "NFC機能をONにしてください", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- NFC ライフサイクル制御 ---

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

    // --- NFC 検知時のコールバック ---

    @Override
    public void onTagDiscovered(Tag tag) {
        feliCaReader.readStudentId(tag, new FeliCaReader.FeliCaCallback() {
            @Override
            public void onSuccess(@NonNull String studentId) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "読み取り成功: " + studentId, Toast.LENGTH_SHORT).show();
                    etUserId.setText(studentId);
                    AuthSessionManager.clearSession(LoginActivity.this);
                    navigateToMenu(studentId);
                });
            }

            @Override
            public void onFailure(@NonNull Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "読み取り失敗: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("LoginNFC", "Error", exception);
                });
            }
        });
    }

    private void attemptPasswordLogin() {
        String userId = etUserId.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (userId.isEmpty()) {
            Toast.makeText(this, "IDを入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.trim().isEmpty()) {
            Toast.makeText(this, "パスワードを入力してください", Toast.LENGTH_SHORT).show();
            return;
        }

        final AuthApiService service;
        try {
            service = ApiClientFactory.createService(this, AuthApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoginSubmitting(true);
        service.login(new LoginRequest(userId, password)).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponse> call, @NonNull Response<TokenResponse> response) {
                setLoginSubmitting(false);
                if (response.isSuccessful() && response.body() != null) {
                    handleLoginSuccess(userId, response.body());
                    return;
                }
                Toast.makeText(LoginActivity.this, extractErrorMessage(response), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponse> call, @NonNull Throwable t) {
                setLoginSubmitting(false);
                String message = t.getMessage();
                Toast.makeText(
                        LoginActivity.this,
                        "通信エラー" + (message != null && !message.isEmpty() ? ": " + message : ""),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void handleLoginSuccess(@NonNull String userId, @NonNull TokenResponse responseBody) {
        String token = responseBody.getToken();
        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "ログインに成功しましたがトークンを取得できませんでした", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthSessionManager.saveSession(this, userId, token.trim());
        navigateToMenu(userId);
    }

    private void setLoginSubmitting(boolean isSubmitting) {
        btnLogin.setEnabled(!isSubmitting);
        btnStudentCardAuth.setEnabled(!isSubmitting && nfcAdapter != null);
        etUserId.setEnabled(!isSubmitting);
        etPassword.setEnabled(!isSubmitting);
    }

    private String extractErrorMessage(@NonNull Response<TokenResponse> response) {
        String defaultMessage;
        if (response.code() == 401) {
            defaultMessage = "IDまたはパスワードが間違っています";
        } else if (response.code() == 400) {
            defaultMessage = "入力内容を確認してください";
        } else {
            defaultMessage = "ログインに失敗しました (" + response.code() + ")";
        }

        if (response.errorBody() == null) {
            return defaultMessage;
        }

        try {
            String rawBody = response.errorBody().string();
            if (rawBody == null || rawBody.trim().isEmpty()) {
                return defaultMessage;
            }

            JSONObject jsonObject = new JSONObject(rawBody);
            String message = jsonObject.optString("message");
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
        } catch (JSONException | IOException ignored) {
        }
        return defaultMessage;
    }

    // --- 画面遷移 ---

    private void navigateToMenu(String userId) {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }
}
