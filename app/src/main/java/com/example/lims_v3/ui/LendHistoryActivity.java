package com.example.lims_v3.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lims_v3.R;
import com.example.lims_v3.network.AssetMasterResponse;
import com.example.lims_v3.network.GenreApiService;
import com.example.lims_v3.network.GenreResponse;
import com.example.lims_v3.network.LendResponse;
import com.example.lims_v3.network.LendingApiService;
import com.example.lims_v3.util.ApiClientFactory;
import com.example.lims_v3.util.GenreCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LendHistoryActivity extends AppCompatActivity {
    private static final String ALL_GENRES_LABEL = "すべて";

    private static final class GenreOption {
        private final Long genreId;
        private final String label;

        private GenreOption(Long genreId, String label) {
            this.genreId = genreId;
            this.label = label;
        }

        private Long getGenreId() {
            return genreId;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private RecyclerView historyRecyclerView;
    private Spinner spinnerGenre;
    private TextView emptyView;
    private TextView summaryView;
    private LendHistoryAdapter adapter;
    private final List<LendResponse> allHistoryItems = new ArrayList<>();
    private final List<GenreOption> genreOptions = new ArrayList<>();
    private final Map<String, Long> genreIdByManagementNumber = new HashMap<>();
    private LendingApiService lendingService;
    private boolean historyLoaded;
    private boolean genreOptionsReady;
    private boolean historyGenresResolved;
    private boolean suppressGenreSelectionCallback;
    private int pendingGenreLookups;
    private boolean hasGenreLookupFailures;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lend_history);

        findViewById(R.id.btnBackHistory).setOnClickListener(v -> finish());

        historyRecyclerView = findViewById(R.id.rvHistory);
        spinnerGenre = findViewById(R.id.spinnerHistoryGenre);
        emptyView = findViewById(R.id.tvHistoryEmpty);
        summaryView = findViewById(R.id.tvHistorySummary);
        adapter = new LendHistoryAdapter(this, this::openDetail);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(adapter);

        bindGenreOptions(new ArrayList<>());
        setupGenreFilter();
        setGenreFilterEnabled(false);
        showLoadingState();
        if (!initializeServices()) {
            return;
        }
        initializeGenres(false);
        fetchLendHistory();
    }

    private boolean initializeServices() {
        try {
            lendingService = ApiClientFactory.createService(this, LendingApiService.class);
            return true;
        } catch (IllegalStateException | IllegalArgumentException exception) {
            showErrorState(exception.getMessage());
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void setupGenreFilter() {
        spinnerGenre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressGenreSelectionCallback || !historyLoaded || !historyGenresResolved) {
                    return;
                }
                applyGenreFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void initializeGenres(boolean forceRefresh) {
        if (!forceRefresh && GenreCache.isInitialized()) {
            bindGenreOptions(GenreCache.getGenres());
            genreOptionsReady = true;
            updateGenreFilterAvailability();
            return;
        }

        final GenreApiService service;
        try {
            service = ApiClientFactory.createService(this, GenreApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        service.getGenres().enqueue(new Callback<List<GenreResponse>>() {
            @Override
            public void onResponse(Call<List<GenreResponse>> call, Response<List<GenreResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(LendHistoryActivity.this, "ジャンル一覧の取得に失敗しました", Toast.LENGTH_SHORT).show();
                    return;
                }

                GenreCache.storeGenres(response.body());
                bindGenreOptions(response.body());
                genreOptionsReady = true;
                updateGenreFilterAvailability();
            }

            @Override
            public void onFailure(Call<List<GenreResponse>> call, Throwable t) {
                Toast.makeText(LendHistoryActivity.this, "ジャンル一覧の取得に失敗しました", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchLendHistory() {
        lendingService.getLendHistory().enqueue(new Callback<List<LendResponse>>() {
            @Override
            public void onResponse(Call<List<LendResponse>> call, Response<List<LendResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    historyLoaded = true;
                    allHistoryItems.clear();
                    allHistoryItems.addAll(response.body());
                    renderHistory(allHistoryItems);
                    resolveGenresForHistory(allHistoryItems);
                } else {
                    showErrorState("履歴を取得できませんでした (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<LendResponse>> call, Throwable t) {
                Log.e("LendHistory", "通信エラー", t);
                showErrorState("通信エラーが発生しました");
            }
        });
    }

    private void renderHistory(@NonNull List<LendResponse> items) {
        adapter.submitItems(items);
        boolean hasSourceItems = !allHistoryItems.isEmpty();
        boolean isEmpty = items.isEmpty();
        summaryView.setText(buildSummaryText(items.size()));
        historyRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setText(hasSourceItems ? "該当する履歴がありません" : "履歴がありません");
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showLoadingState() {
        summaryView.setText("読み込み中...");
        historyRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void showErrorState(@NonNull String message) {
        historyLoaded = false;
        summaryView.setText("-");
        historyRecyclerView.setVisibility(View.GONE);
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
        setGenreFilterEnabled(false);
    }

    private void resolveGenresForHistory(@NonNull List<LendResponse> items) {
        genreIdByManagementNumber.clear();
        historyGenresResolved = false;
        hasGenreLookupFailures = false;

        Set<String> managementNumbers = new LinkedHashSet<>();
        for (LendResponse item : items) {
            String managementNumber = normalizeManagementNumber(item.getManagementNumber());
            if (!managementNumber.isEmpty()) {
                managementNumbers.add(managementNumber);
            }
        }

        if (managementNumbers.isEmpty()) {
            historyGenresResolved = true;
            updateGenreFilterAvailability();
            return;
        }

        pendingGenreLookups = managementNumbers.size();
        for (String managementNumber : managementNumbers) {
            fetchAssetGenre(managementNumber);
        }
    }

    private void fetchAssetGenre(@NonNull String managementNumber) {
        lendingService.getAssetMaster(managementNumber).enqueue(new Callback<AssetMasterResponse>() {
            @Override
            public void onResponse(Call<AssetMasterResponse> call, Response<AssetMasterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genreIdByManagementNumber.put(managementNumber, resolveGenreId(response.body()));
                } else {
                    hasGenreLookupFailures = true;
                }
                finishGenreLookup();
            }

            @Override
            public void onFailure(Call<AssetMasterResponse> call, Throwable t) {
                hasGenreLookupFailures = true;
                finishGenreLookup();
            }
        });
    }

    private void finishGenreLookup() {
        pendingGenreLookups--;
        if (pendingGenreLookups > 0) {
            return;
        }

        historyGenresResolved = true;
        updateGenreFilterAvailability();
        if (hasGenreLookupFailures) {
            Toast.makeText(this, "一部のジャンル情報を取得できませんでした", Toast.LENGTH_SHORT).show();
        }
    }

    private Long resolveGenreId(@NonNull AssetMasterResponse assetMaster) {
        if (assetMaster.getGenreId() != null) {
            return assetMaster.getGenreId();
        }

        String genreName = assetMaster.getGenreName();
        if (genreName == null || genreName.trim().isEmpty()) {
            return null;
        }

        for (GenreResponse genre : GenreCache.getGenres()) {
            if (genre == null) {
                continue;
            }
            String candidateName = genre.getGenreName();
            if (candidateName != null && candidateName.equals(genreName)) {
                return genre.getGenreId();
            }
            String candidateCode = genre.getGenreCode();
            if (candidateCode != null && candidateCode.equals(genreName)) {
                return genre.getGenreId();
            }
        }
        return null;
    }

    private void bindGenreOptions(@NonNull List<GenreResponse> genres) {
        genreOptions.clear();
        genreOptions.add(new GenreOption(null, ALL_GENRES_LABEL));
        for (GenreResponse genre : genres) {
            if (genre == null || genre.isDisabled()) {
                continue;
            }

            String label = genre.getGenreName();
            if (label == null || label.trim().isEmpty()) {
                label = genre.getGenreCode();
            }
            if (label == null || label.trim().isEmpty()) {
                label = "ジャンル " + genre.getGenreId();
            }
            genreOptions.add(new GenreOption(genre.getGenreId(), label));
        }

        suppressGenreSelectionCallback = true;
        ArrayAdapter<GenreOption> genreAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                genreOptions
        );
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenre.setAdapter(genreAdapter);
        spinnerGenre.setSelection(0, false);
        suppressGenreSelectionCallback = false;
    }

    private void updateGenreFilterAvailability() {
        boolean enabled = genreOptionsReady && historyGenresResolved;
        setGenreFilterEnabled(enabled);
        if (enabled) {
            applyGenreFilter();
        }
    }

    private void setGenreFilterEnabled(boolean enabled) {
        spinnerGenre.setEnabled(enabled);
    }

    private void applyGenreFilter() {
        GenreOption selectedGenre = (GenreOption) spinnerGenre.getSelectedItem();
        if (selectedGenre == null || selectedGenre.getGenreId() == null) {
            renderHistory(allHistoryItems);
            return;
        }

        List<LendResponse> filteredItems = new ArrayList<>();
        for (LendResponse item : allHistoryItems) {
            Long itemGenreId = genreIdByManagementNumber.get(normalizeManagementNumber(item.getManagementNumber()));
            if (selectedGenre.getGenreId().equals(itemGenreId)) {
                filteredItems.add(item);
            }
        }
        renderHistory(filteredItems);
    }

    private String buildSummaryText(int visibleCount) {
        GenreOption selectedGenre = (GenreOption) spinnerGenre.getSelectedItem();
        if (selectedGenre != null && selectedGenre.getGenreId() != null) {
            return visibleCount + "件 / 全" + allHistoryItems.size() + "件";
        }
        return visibleCount + "件";
    }

    @NonNull
    private String normalizeManagementNumber(String managementNumber) {
        return managementNumber == null ? "" : managementNumber.trim();
    }

    private void openDetail(@NonNull LendResponse item) {
        Intent intent = new Intent(this, LendDetailActivity.class);
        intent.putExtra("LEND_ULID", item.getLendUlid());
        startActivity(intent);
    }
}
