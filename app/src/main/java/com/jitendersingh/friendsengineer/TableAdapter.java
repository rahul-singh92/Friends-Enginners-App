package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {

    private List<String> tables;
    private OnTableClickListener listener;

    public interface OnTableClickListener {
        void onTableClick(String tableName);
    }

    public TableAdapter(List<String> tables, OnTableClickListener listener) {
        this.tables = tables;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String tableName = tables.get(position);

        // Format table name for display
        String displayName = formatTableName(tableName);
        holder.tableNameText.setText(displayName);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTableClick(tableName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tables.size();
    }

    private String formatTableName(String name) {
        // Replace underscores with spaces and capitalize
        return name.replace("_", " ").toUpperCase();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tableNameText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tableNameText = itemView.findViewById(R.id.tableName);
        }
    }
}
