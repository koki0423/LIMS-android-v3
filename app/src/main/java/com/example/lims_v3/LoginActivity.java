package com.example.lims_v3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.ui.MenuActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etUserId = findViewById(R.id.etUserId);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnStudentCardAuth = findViewById(R.id.btnStudentCardAuth);

        // ID/PASSログイン処理
        btnLogin.setOnClickListener(v -> {
            String userId = etUserId.getText().toString();
            // TODO: ここでDB照合を行う [cite: 18]
            if (!userId.isEmpty()) {
                navigateToMenu(userId);
            } else {
                Toast.makeText(this, "IDを入力してください", Toast.LENGTH_SHORT).show();
            }
        });

        // 学生証認証処理
        btnStudentCardAuth.setOnClickListener(v -> {
            // TODO: NFC読み取り処理の実装 [cite: 20]
            navigateToMenu("Student001");
        });
    }

    private void navigateToMenu(String userId) {
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
        finish();
    }
}