package com.example.lims_v3.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.lims_v3.R;

public class NfcTestActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private TextView tvResult;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_test);

        tvResult = findViewById(R.id.tvNfcResult);
        tvStatus = findViewById(R.id.tvNfcStatus);

        findViewById(R.id.btnBackNfcTest).setOnClickListener(v -> finish());

        // NFCアダプターの取得
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            tvStatus.setText("ステータス: 非対応端末です");
        } else if (!nfcAdapter.isEnabled()) {
            tvStatus.setText("ステータス: NFCが無効です。設定からONにしてください");
        } else {
            tvStatus.setText("ステータス: 待機中...タグをかざしてください");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            // アプリが前面にいる時だけNFCを横取りする設定
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_MUTABLE); // Android 12以降対応

            IntentFilter[] filters = new IntentFilter[]{};
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, filters, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    // タグ検出時に呼ばれる
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            // タグID取得
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                byte[] idBytes = tag.getId();
                String hexId = bytesToHex(idBytes);

                tvResult.setText(hexId);
                tvStatus.setText("読み取り成功！");
                Toast.makeText(this, "検出: " + hexId, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // バイト配列を16進数文字列("0F2A...")に変換する便利関数
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}