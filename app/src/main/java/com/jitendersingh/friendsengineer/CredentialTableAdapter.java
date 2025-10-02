package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CredentialTableAdapter extends RecyclerView.Adapter<CredentialTableAdapter.ViewHolder> {

    private List<String> tables;
    private OnTableClickListener listener;

    public interface OnTableClickListener {
        void onTableClick(String tableName);
    }

    public CredentialTableAdapter(List<String> tables, OnTableClickListener listener) {
        this.tables = tables;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_credential_table, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String tableName = tables.get(position);

        // Format table name for display
        String displayName = formatTableName(tableName);
        holder.tableNameText.setText(displayName);

        // Set description based on table type
        String description = getTableDescription(tableName);
        holder.descriptionText.setText(description);

        // Set icon color based on table type
        if (tableName.contains("admin")) {
            holder.iconContainer.setBackgroundResource(R.drawable.win11_admin_icon_bg);
        } else {
            holder.iconContainer.setBackgroundResource(R.drawable.win11_worker_icon_bg);
        }

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

    private String formatTableName(String tableName) {
        // Remove "credentials_" prefix and format
        if (tableName.startsWith("credentials_")) {
            tableName = tableName.substring(12);
        }
        // Capitalize first letter
        return tableName.substring(0, 1).toUpperCase() + tableName.substring(1) + " Credentials";
    }

    private String getTableDescription(String tableName) {
        if (tableName.contains("admin")) {
            return "Manage administrator accounts";
        } else if (tableName.contains("worker")) {
            return "Manage worker accounts";
        } else {
            return "Manage user credentials";
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tableNameText;
        TextView descriptionText;
        LinearLayout iconContainer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tableNameText = itemView.findViewById(R.id.credentialTableName);
            descriptionText = itemView.findViewById(R.id.credentialTableDescription);
            iconContainer = itemView.findViewById(R.id.credentialIconContainer);
        }
    }
}
