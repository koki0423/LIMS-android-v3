package com.example.lims_v3.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lims_v3.R;
import com.example.lims_v3.network.LendResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LendHistoryAdapter extends RecyclerView.Adapter<LendHistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull LendResponse item);
    }

    private final LayoutInflater inflater;
    private final List<LendResponse> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());

    public LendHistoryAdapter(@NonNull Context context, @NonNull OnItemClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void submitItems(@NonNull List<LendResponse> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_lend_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LendResponse item = items.get(position);
        boolean returned = item.getReturned();

        holder.tvManagementNumber.setText(getDisplayText(item.getManagementNumber()));
        holder.tvBorrower.setText(getDisplayText(item.getBorrowerId()));
        holder.tvDate.setText(item.getLentAt() != null ? dateFormat.format(item.getLentAt()) : "-");
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvStatus.setText(returned ? "返却済み" : "貸出中");
        holder.tvStatus.setBackgroundResource(returned
                ? R.drawable.bg_lend_status_returned
                : R.drawable.bg_lend_status_active);
        holder.tvStatus.setTextColor(returned ? 0xFF424242 : 0xFF6D4C41);

        String note = item.getNote();
        if (note != null && !note.trim().isEmpty()) {
            holder.tvNote.setText("備考: " + note.trim());
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getDisplayText(String value) {
        return value != null && !value.trim().isEmpty() ? value : "-";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvManagementNumber;
        private final TextView tvBorrower;
        private final TextView tvDate;
        private final TextView tvQuantity;
        private final TextView tvStatus;
        private final TextView tvNote;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvManagementNumber = itemView.findViewById(R.id.tvItemManagementNumber);
            tvBorrower = itemView.findViewById(R.id.tvItemBorrower);
            tvDate = itemView.findViewById(R.id.tvItemDate);
            tvQuantity = itemView.findViewById(R.id.tvItemQuantity);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);
            tvNote = itemView.findViewById(R.id.tvItemNote);
        }
    }
}
