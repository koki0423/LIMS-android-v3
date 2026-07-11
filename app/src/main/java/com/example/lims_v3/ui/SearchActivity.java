package com.example.lims_v3.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lims_v3.R;
import com.example.lims_v3.network.AssetSetResponse;
import com.example.lims_v3.network.GenreApiService;
import com.example.lims_v3.network.GenreResponse;
import com.example.lims_v3.network.SearchApiService;
import com.example.lims_v3.util.ApiClientFactory;
import com.example.lims_v3.util.GenreCache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity {
    private static final String ALL_GENRES_LABEL = "すべて";
    private static final String DEFAULT_PROMPT_MESSAGE = "管理番号または備品名を入力してください";
    private static final String MANAGEMENT_NUMBER_HINT = "例: OFS-20250901-0001";
    private static final String ASSET_NAME_HINT = "例: MacBook";
    private static final String MANAGEMENT_NUMBER_NOTE = "管理番号は完全一致で検索します";
    private static final String ASSET_NAME_NOTE = "備品名は部分一致で検索します";

    private enum SearchMode {
        MANAGEMENT_NUMBER,
        ASSET_NAME
    }

    private enum ScreenState {
        READY,
        SEARCHING
    }

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

    private RadioGroup rgSearchTarget;
    private EditText etQuery;
    private TextView tvSearchModeNote;
    private Spinner spinnerGenre;
    private LinearLayout resultContainer;
    private TextView tvSearchSummary;
    private TextView tvInitStatus;
    private ProgressBar progressInit;
    private LinearLayout initErrorContainer;
    private TextView tvInitError;
    private Button btnRetryInit;
    private Button btnExec;
    private Button btnClear;
    private SearchMode currentMode = SearchMode.MANAGEMENT_NUMBER;
    private ScreenState screenState = ScreenState.READY;
    private final List<GenreOption> genreOptions = new ArrayList<>();
    private boolean isGenreLoading = false;
    private boolean isGenreAvailable = false;
    private boolean hasGenreLoadError = false;
    private String genreLoadErrorMessage = "";
    private boolean hasBlockingSetupError = false;
    private String blockingSetupErrorMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        rgSearchTarget = findViewById(R.id.rgSearchTarget);
        etQuery = findViewById(R.id.etSearchQuery);
        tvSearchModeNote = findViewById(R.id.tvSearchModeNote);
        spinnerGenre = findViewById(R.id.spinnerGenre);
        resultContainer = findViewById(R.id.searchResultContainer);
        tvSearchSummary = findViewById(R.id.tvSearchSummary);
        tvInitStatus = findViewById(R.id.tvSearchInitStatus);
        progressInit = findViewById(R.id.progressSearchInit);
        initErrorContainer = findViewById(R.id.layoutSearchInitError);
        tvInitError = findViewById(R.id.tvSearchInitError);
        btnRetryInit = findViewById(R.id.btnRetrySearchInit);
        btnExec = findViewById(R.id.btnExecSearch);
        btnClear = findViewById(R.id.btnClearSearch);

        setupSearchTarget();
        setupInput();
        updateSearchModeUi();
        updateActionButtons();

        findViewById(R.id.btnBackSearch).setOnClickListener(v -> finish());
        btnRetryInit.setOnClickListener(v -> initializeGenres(true));

        btnExec.setOnClickListener(v -> {
            String query = etQuery.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, DEFAULT_PROMPT_MESSAGE, Toast.LENGTH_SHORT).show();
                return;
            }

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            performSearch(query);
        });

        btnClear.setOnClickListener(v -> resetSearchForm());
        bindGenrePlaceholder();
        showReadyState(true);
        initializeGenres(false);
    }

    private void setupSearchTarget() {
        rgSearchTarget.setOnCheckedChangeListener((group, checkedId) -> {
            currentMode = checkedId == R.id.rbSearchByName
                    ? SearchMode.ASSET_NAME
                    : SearchMode.MANAGEMENT_NUMBER;
            updateSearchModeUi();
            if (screenState == ScreenState.READY) {
                renderMessage(DEFAULT_PROMPT_MESSAGE);
            }
            updateActionButtons();
        });
    }

    private void setupInput() {
        etQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateActionButtons();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        spinnerGenre.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateActionButtons();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateActionButtons();
            }
        });
    }

    private void updateSearchModeUi() {
        if (currentMode == SearchMode.ASSET_NAME) {
            etQuery.setHint(ASSET_NAME_HINT);
            tvSearchModeNote.setText(ASSET_NAME_NOTE);
        } else {
            etQuery.setHint(MANAGEMENT_NUMBER_HINT);
            tvSearchModeNote.setText(MANAGEMENT_NUMBER_NOTE);
        }
    }

    private void updateActionButtons() {
        boolean canInteract = screenState != ScreenState.SEARCHING && !hasBlockingSetupError;
        boolean hasQuery = !etQuery.getText().toString().trim().isEmpty();
        boolean hasGenreFilter = hasSelectedGenreFilter();
        boolean hasRenderedState = !"-".contentEquals(tvSearchSummary.getText());

        btnExec.setEnabled(canInteract && hasQuery);
        btnClear.setEnabled(canInteract && (hasQuery || hasGenreFilter || hasRenderedState));
        btnRetryInit.setEnabled(canInteract && hasGenreLoadError);
    }

    private void resetSearchForm() {
        etQuery.setText("");
        rgSearchTarget.check(R.id.rbSearchByManagementNumber);
        if (spinnerGenre.getAdapter() != null && spinnerGenre.getAdapter().getCount() > 0) {
            spinnerGenre.setSelection(0);
        }
        currentMode = SearchMode.MANAGEMENT_NUMBER;
        updateSearchModeUi();
        if (screenState == ScreenState.READY) {
            renderMessage(DEFAULT_PROMPT_MESSAGE);
        }
        updateActionButtons();
    }

    private void initializeGenres(boolean forceRefresh) {
        if (!forceRefresh && GenreCache.isInitialized()) {
            bindGenreOptions(GenreCache.getGenres());
            showGenreReadyState();
            return;
        }

        showInitializationLoading();

        final GenreApiService service;
        try {
            service = ApiClientFactory.createService(this, GenreApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            showBlockingSetupError(exception.getMessage());
            return;
        }

        service.getGenres().enqueue(new Callback<List<GenreResponse>>() {
            @Override
            public void onResponse(Call<List<GenreResponse>> call, Response<List<GenreResponse>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    showInitializationError("ジャンル一覧の取得に失敗しました (" + response.code() + ")");
                    return;
                }

                GenreCache.storeGenres(response.body());
                bindGenreOptions(response.body());
                showGenreReadyState();
            }

            @Override
            public void onFailure(Call<List<GenreResponse>> call, Throwable t) {
                showInitializationError("ジャンル一覧の取得に失敗しました");
            }
        });
    }

    private void bindGenrePlaceholder() {
        genreOptions.clear();
        genreOptions.add(new GenreOption(null, ALL_GENRES_LABEL));
        applyGenreAdapter();
        spinnerGenre.setSelection(0);
    }

    private void bindGenreOptions(List<GenreResponse> genres) {
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

        applyGenreAdapter();
        spinnerGenre.setSelection(0);
    }

    private void applyGenreAdapter() {
        ArrayAdapter<GenreOption> genreAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                genreOptions
        );
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGenre.setAdapter(genreAdapter);
    }

    private void performSearch(String query) {
        showSearchingState();

        final SearchApiService service;
        try {
            service = ApiClientFactory.createService(this, SearchApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            showSearchError(exception.getMessage());
            return;
        }

        service.searchAssets(buildSearchQueryMap(query)).enqueue(new Callback<List<AssetSetResponse>>() {
            @Override
            public void onResponse(Call<List<AssetSetResponse>> call, Response<List<AssetSetResponse>> response) {
                if (!response.isSuccessful()) {
                    showSearchError("検索失敗: " + response.code());
                    return;
                }

                List<AssetSetResponse> results = response.body() != null
                        ? response.body()
                        : new ArrayList<>();
                showReadyState(false);
                renderResults(results);

                if (currentMode == SearchMode.MANAGEMENT_NUMBER && results.size() == 1) {
                    openDetail(results.get(0));
                }
                updateActionButtons();
            }

            @Override
            public void onFailure(Call<List<AssetSetResponse>> call, Throwable t) {
                showSearchError("通信エラー: " + t.getMessage());
            }
        });
    }

    private Map<String, String> buildSearchQueryMap(String query) {
        Map<String, String> queryMap = new LinkedHashMap<>();
        if (currentMode == SearchMode.ASSET_NAME) {
            queryMap.put("name", query);
        } else {
            queryMap.put("management_number", query);
        }

        GenreOption selectedGenre = (GenreOption) spinnerGenre.getSelectedItem();
        if (selectedGenre != null && selectedGenre.getGenreId() != null) {
            queryMap.put("genre_id", String.valueOf(selectedGenre.getGenreId()));
        }
        return queryMap;
    }

    private void showInitializationLoading() {
        isGenreLoading = true;
        isGenreAvailable = false;
        hasGenreLoadError = false;
        genreLoadErrorMessage = "";
        hasBlockingSetupError = false;
        blockingSetupErrorMessage = "";
        renderHeaderState();
        updateFormEnabled();
        updateActionButtons();
    }

    private void showGenreReadyState() {
        isGenreLoading = false;
        isGenreAvailable = true;
        hasGenreLoadError = false;
        genreLoadErrorMessage = "";
        hasBlockingSetupError = false;
        blockingSetupErrorMessage = "";
        renderHeaderState();
        updateFormEnabled();
        updateActionButtons();
    }

    private void showReadyState(boolean showPromptMessage) {
        screenState = ScreenState.READY;
        renderHeaderState();
        updateFormEnabled();
        if (showPromptMessage) {
            renderMessage(DEFAULT_PROMPT_MESSAGE);
        }
        updateActionButtons();
    }

    private void showInitializationError(String message) {
        isGenreLoading = false;
        isGenreAvailable = false;
        hasGenreLoadError = true;
        genreLoadErrorMessage = message;
        hasBlockingSetupError = false;
        blockingSetupErrorMessage = "";
        bindGenrePlaceholder();
        renderHeaderState();
        updateFormEnabled();
        updateActionButtons();
    }

    private void showBlockingSetupError(String message) {
        isGenreLoading = false;
        isGenreAvailable = false;
        hasGenreLoadError = false;
        genreLoadErrorMessage = "";
        hasBlockingSetupError = true;
        blockingSetupErrorMessage = message;
        bindGenrePlaceholder();
        renderHeaderState();
        updateFormEnabled();
        updateActionButtons();
        renderMessage("API設定を確認してください");
    }

    private void showSearchingState() {
        screenState = ScreenState.SEARCHING;
        renderHeaderState();
        updateFormEnabled();
        renderMessage("検索中です");
        updateActionButtons();
    }

    private void showSearchError(String message) {
        showReadyState(false);
        renderMessage(message);
        updateActionButtons();
    }

    private void renderHeaderState() {
        if (screenState == ScreenState.SEARCHING) {
            progressInit.setVisibility(View.VISIBLE);
            tvInitStatus.setVisibility(View.VISIBLE);
            tvInitStatus.setText("検索中です");
            initErrorContainer.setVisibility(View.GONE);
            return;
        }

        if (isGenreLoading) {
            progressInit.setVisibility(View.VISIBLE);
            tvInitStatus.setVisibility(View.VISIBLE);
            tvInitStatus.setText("ジャンル一覧を取得しています");
            initErrorContainer.setVisibility(View.GONE);
            return;
        }

        progressInit.setVisibility(View.GONE);
        tvInitStatus.setVisibility(View.GONE);
        if (hasBlockingSetupError) {
            initErrorContainer.setVisibility(View.VISIBLE);
            tvInitError.setText(blockingSetupErrorMessage);
            return;
        }
        if (hasGenreLoadError) {
            initErrorContainer.setVisibility(View.VISIBLE);
            tvInitError.setText(genreLoadErrorMessage + "\nジャンル指定なしで検索できます");
        } else {
            initErrorContainer.setVisibility(View.GONE);
        }
    }

    private void updateFormEnabled() {
        boolean enabled = screenState != ScreenState.SEARCHING && !hasBlockingSetupError;
        setRadioGroupEnabled(rgSearchTarget, enabled);
        etQuery.setEnabled(enabled);
        spinnerGenre.setEnabled(enabled && isGenreAvailable);
    }

    private void renderResults(List<AssetSetResponse> results) {
        resultContainer.removeAllViews();
        if (results.isEmpty()) {
            tvSearchSummary.setText("0件");
            addMessageView("該当する備品が見つかりませんでした");
            return;
        }

        tvSearchSummary.setText(results.size() + "件");
        for (AssetSetResponse item : results) {
            addCard(item);
        }
    }

    private void renderMessage(String message) {
        resultContainer.removeAllViews();
        tvSearchSummary.setText("-");
        addMessageView(message);
    }

    private void addMessageView(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setPadding(16, 16, 16, 16);
        resultContainer.addView(tv);
    }

    private void setRadioGroupEnabled(RadioGroup radioGroup, boolean enabled) {
        radioGroup.setEnabled(enabled);
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(enabled);
        }
    }

    private boolean hasSelectedGenreFilter() {
        GenreOption selectedGenre = (GenreOption) spinnerGenre.getSelectedItem();
        return selectedGenre != null && selectedGenre.getGenreId() != null;
    }

    private String getGenre(AssetSetResponse item) {
        if (item == null || item.getMaster() == null) {
            return "";
        }
        String genreName = item.getMaster().getGenreName();
        if (genreName != null && !genreName.isEmpty()) {
            return genreName;
        }
        return GenreCache.findGenreNameById(item.getMaster().getGenreId());
    }

    private void addCard(AssetSetResponse item) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.item_asset_search, resultContainer, false);

        TextView tvName = card.findViewById(R.id.tvAssetName);
        TextView tvMng = card.findViewById(R.id.tvAssetMngNum);
        TextView tvLoc = card.findViewById(R.id.tvAssetLocation);
        TextView tvQty = card.findViewById(R.id.tvAssetQuantity);
        TextView tvGenre = card.findViewById(R.id.tvAssetGenre);

        tvName.setText("-");
        tvMng.setText("-");
        tvLoc.setText("-");
        tvQty.setText("-");
        tvGenre.setVisibility(View.GONE);

        if (item.getMaster() != null) {
            String assetName = item.getMaster().getName();
            if ((assetName == null || assetName.isEmpty()) && item.getAsset() != null) {
                assetName = item.getAsset().getName();
            }
            tvName.setText(assetName != null && !assetName.isEmpty() ? assetName : "-");
            String genreName = getGenre(item);
            if (genreName.isEmpty()) {
                tvGenre.setVisibility(View.GONE);
            } else {
                tvGenre.setVisibility(View.VISIBLE);
                tvGenre.setText(genreName);
            }
        }
        if (item.getAsset() != null) {
            tvMng.setText(item.getAsset().getManagementNumber() != null ? item.getAsset().getManagementNumber() : "-");
            tvLoc.setText(item.getAsset().getLocation() != null ? item.getAsset().getLocation() : "-");
            tvQty.setText(String.valueOf(item.getAsset().getQuantity()));
        }

        card.setOnClickListener(v -> openDetail(item));

        resultContainer.addView(card);
    }

    private void openDetail(AssetSetResponse item) {
        Intent intent = new Intent(SearchActivity.this, AssetDetailActivity.class);
        intent.putExtra("ASSET_DATA", item);
        startActivity(intent);
    }
}
