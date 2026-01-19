package com.example.lims_v3.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.AssetSetResponse;
import com.example.lims_v3.network.SearchApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchActivity extends AppCompatActivity {

    private EditText etId;
    private EditText etKeyword;
    private LinearLayout resultContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etId = findViewById(R.id.etSearchId);
        etKeyword = findViewById(R.id.etSearchKeyword);
        resultContainer = findViewById(R.id.searchResultContainer);

        findViewById(R.id.btnBackSearch).setOnClickListener(v -> finish());

        Button btnExec = findViewById(R.id.btnExecSearch);
        btnExec.setOnClickListener(v -> {
            // 入力チェックと優先順位の決定
            String idInput = etId.getText().toString().trim();
            String keywordInput = etKeyword.getText().toString().trim();
            String finalQuery = "";

            if (!keywordInput.isEmpty()) {
                finalQuery = keywordInput;
            } else if (!idInput.isEmpty()) {
                finalQuery = idInput;
            } else {
                Toast.makeText(this, "検索条件を入力してください", Toast.LENGTH_SHORT).show();
                return;
            }

            // キーボードを閉じる
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            // 検索実行
            performSearch(finalQuery);
        });
    }

    private void performSearch(String query) {
        resultContainer.removeAllViews(); // クリア

        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(SettingsActivity.KEY_API_URL, "");
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        SearchApiService service = retrofit.create(SearchApiService.class);

        // APIコール
        service.searchAssets(query).enqueue(new Callback<List<AssetSetResponse>>() {
            @Override
            public void onResponse(Call<List<AssetSetResponse>> call, Response<List<AssetSetResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<AssetSetResponse> results = response.body();
                    handleResults(results);
                } else {
                    Toast.makeText(SearchActivity.this, "エラー: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AssetSetResponse>> call, Throwable t) {
                Toast.makeText(SearchActivity.this, "通信エラー: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleResults(List<AssetSetResponse> results) {
        if (results.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("該当する備品が見つかりませんでした");
            tv.setPadding(16, 16, 16, 16);
            resultContainer.addView(tv);
            return;
        }

        // ★ 1件ヒットなら即詳細へジャンプ
        if (results.size() == 1) {
            openDetail(results.get(0));
            // 戻ってきたとき用にカードも表示しておく
            addCard(results.get(0));
            return;
        }

        // 複数ヒットならリスト表示
        for (AssetSetResponse item : results) {
            addCard(item);
        }
    }

    private void addCard(AssetSetResponse item) {
        // カードのレイアウト (item_asset_search.xml) を読み込み
        LayoutInflater inflater = LayoutInflater.from(this);
        // ※ item_asset_search.xml がまだなければ作成してください（前回提示したものです）
        View card = inflater.inflate(R.layout.item_asset_search, resultContainer, false);

        TextView tvName = card.findViewById(R.id.tvAssetName);
        TextView tvMng = card.findViewById(R.id.tvAssetMngNum);
        TextView tvModel = card.findViewById(R.id.tvAssetModel);
        TextView tvLoc = card.findViewById(R.id.tvAssetLocation);
        TextView tvQty = card.findViewById(R.id.tvAssetQuantity);

        // データセット (Nullチェック安全策)
        if (item.getMaster() != null) {
            tvName.setText(item.getMaster().getName());
            tvModel.setText(item.getMaster().getModel() != null ? item.getMaster().getModel() : "-");
        }
        if (item.getAsset() != null) {
            tvMng.setText(item.getAsset().getManagementNumber());
            tvLoc.setText(item.getAsset().getLocation());
            tvQty.setText(String.valueOf(item.getAsset().getQuantity()));
        }

        // クリックで詳細へ
        card.setOnClickListener(v -> openDetail(item));

        resultContainer.addView(card);
    }

    private void openDetail(AssetSetResponse item) {
        Intent intent = new Intent(SearchActivity.this, AssetDetailActivity.class);
        // オブジェクトごと渡す
        intent.putExtra("ASSET_DATA", item);
        startActivity(intent);
    }
}