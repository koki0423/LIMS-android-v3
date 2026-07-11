package com.example.lims_v3.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lims_v3.R;
import com.example.lims_v3.network.ReturnResponse;
import com.example.lims_v3.network.ReturningApiService;
import com.example.lims_v3.util.ApiClientFactory;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReturnHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecyclerView;
    private TextView emptyView;
    private TextView summaryView;
    private ReturnHistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_history);

        findViewById(R.id.btnBackReturnHistory).setOnClickListener(v -> finish());

        historyRecyclerView = findViewById(R.id.rvReturnHistory);
        emptyView = findViewById(R.id.tvReturnHistoryEmpty);
        summaryView = findViewById(R.id.tvReturnHistorySummary);
        adapter = new ReturnHistoryAdapter(this);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(adapter);

        showLoadingState();
        fetchHistory();
    }

    private void fetchHistory() {
        final ReturningApiService service;
        try {
            service = ApiClientFactory.createService(this, ReturningApiService.class);
        } catch (IllegalStateException | IllegalArgumentException exception) {
            showErrorState(exception.getMessage());
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        service.getReturnHistory().enqueue(new Callback<List<ReturnResponse>>() {
            @Override
            public void onResponse(Call<List<ReturnResponse>> call, Response<List<ReturnResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderHistory(response.body());
                } else {
                    showErrorState("返却ログを取得できませんでした (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<ReturnResponse>> call, Throwable t) {
                Log.e("ReturnHistory", "通信エラー", t);
                showErrorState("通信エラーが発生しました");
            }
        });
    }

    private void renderHistory(List<ReturnResponse> items) {
        adapter.submitItems(items);
        boolean isEmpty = items.isEmpty();
        summaryView.setText(items.size() + "件");
        historyRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyView.setText("返却ログがありません");
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showLoadingState() {
        summaryView.setText("読み込み中...");
        historyRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        summaryView.setText("-");
        historyRecyclerView.setVisibility(View.GONE);
        emptyView.setText(message);
        emptyView.setVisibility(View.VISIBLE);
    }
}
