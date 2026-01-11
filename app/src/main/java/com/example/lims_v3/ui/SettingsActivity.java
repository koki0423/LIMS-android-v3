package com.example.lims_v3.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 戻るボタン
        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());
    }
}
