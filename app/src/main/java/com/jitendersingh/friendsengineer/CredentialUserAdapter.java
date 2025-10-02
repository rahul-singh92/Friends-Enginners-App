package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class CredentialUserAdapter extends RecyclerView.Adapter<CredentialUserAdapter.ViewHolder> {

    private List<String> documentIds;
    private Map<String, Map<String, Object>> documentsData;
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onItemLongClick(int position, String docId, Map<String, Object> data);
    }

    public CredentialUserAdapter(List<String> documentIds, Map<String, Map<String, Object>> documentsData, OnItemActionListener listener) {
        this.documentIds = documentIds;
        this.documentsData = documentsData;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_credential_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String docId = documentIds.get(position);
        Map<String, Object> data = documentsData.get(docId);

        if (data != null) {
            String username = (String) data.get("Username");
            String password = (String) data.get("Password");
            String name = (String) data.get("Name");
            String fatherName = (String) data.get("FATHER_S_NAME");

            if (username == null) username = "N/A";
            if (password == null) password = "N/A";
            if (name == null) name = "N/A";
            if (fatherName == null) fatherName = "N/A";

            // Set name
            holder.nameText.setText(name);

            // Set initial
            if (!name.equals("N/A") && !name.isEmpty()) {
                holder.initialText.setText(String.valueOf(name.charAt(0)).toUpperCase());
            } else {
                holder.initialText.setText("?");
            }

            // Set credentials
            holder.usernameText.setText(username);
            holder.passwordText.setText(password);
            holder.fatherNameText.setText(fatherName);

            // More options click
            holder.moreOptions.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(position, docId, data);
                }
            });

            // Long press on entire item
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(position, docId, data);
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public int getItemCount() {
        return documentIds.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView usernameText;
        TextView passwordText;
        TextView fatherNameText;
        TextView initialText;
        ImageView moreOptions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.userName);
            usernameText = itemView.findViewById(R.id.userUsername);
            passwordText = itemView.findViewById(R.id.userPassword);
            fatherNameText = itemView.findViewById(R.id.userFatherName);
            initialText = itemView.findViewById(R.id.userInitial);
            moreOptions = itemView.findViewById(R.id.moreOptions);
        }
    }
}
