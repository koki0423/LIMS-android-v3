package com.example.lims_v3.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;

public class SearchActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 戻るボタン
        findViewById(R.id.btnBackSearch).setOnClickListener(v -> finish());
    }
}
