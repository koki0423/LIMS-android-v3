package com.example.lims_v3.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;

public class ReturnHistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lending_history);

        // 戻るボタン
        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());
    }
}
