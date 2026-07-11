package com.example.lims_v3.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lims_v3.R;
import com.example.lims_v3.network.ReturnResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReturnHistoryAdapter extends RecyclerView.Adapter<ReturnHistoryAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<ReturnResponse> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault());

    public ReturnHistoryAdapter(@NonNull Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void submitItems(@NonNull List<ReturnResponse> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_return_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReturnResponse item = items.get(position);
        holder.tvReturnDate.setText(item.getReturnedAt() != null ? dateFormat.format(item.getReturnedAt()) : "-");
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvProcessedBy.setText(getDisplayText(item.getProcessedById()));
        holder.tvReturnUlid.setText(getDisplayText(item.getReturnUlid()));

        String note = item.getNote();
        if (note != null && !note.trim().isEmpty()) {
            holder.tvNote.setText("備考: " + note.trim());
            holder.tvNote.setVisibility(View.VISIBLE);
        } else {
            holder.tvNote.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String getDisplayText(String value) {
        return value != null && !value.trim().isEmpty() ? value : "-";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvReturnDate;
        private final TextView tvReturnUlid;
        private final TextView tvQuantity;
        private final TextView tvProcessedBy;
        private final TextView tvNote;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReturnDate = itemView.findViewById(R.id.tvReturnDate);
            tvReturnUlid = itemView.findViewById(R.id.tvReturnUlid);
            tvQuantity = itemView.findViewById(R.id.tvReturnQuantity);
            tvProcessedBy = itemView.findViewById(R.id.tvProcessedBy);
            tvNote = itemView.findViewById(R.id.tvReturnNote);
        }
    }
}
