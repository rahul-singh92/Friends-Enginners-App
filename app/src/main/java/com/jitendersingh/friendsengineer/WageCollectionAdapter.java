package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WageCollectionAdapter extends RecyclerView.Adapter<WageCollectionAdapter.ViewHolder> {

    private List<String> collections;
    private OnCollectionClickListener listener;

    public interface OnCollectionClickListener {
        void onCollectionClick(String collectionName);
    }

    public WageCollectionAdapter(List<String> collections, OnCollectionClickListener listener) {
        this.collections = collections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pdf_collection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String collectionName = collections.get(position);

        // Format collection name for display
        String displayName = formatCollectionName(collectionName);
        holder.collectionNameText.setText(displayName);

        // Show a subtitle
        holder.collectionDateText.setText("Tap to view workers");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCollectionClick(collectionName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }

    private String formatCollectionName(String name) {
        // Remove "wage_collection_" prefix if exists
        if (name.startsWith("wage_collection_")) {
            name = name.substring(16);
        }
        // Replace underscores with spaces and capitalize
        return name.replace("_", " ").toUpperCase();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView collectionNameText;
        TextView collectionDateText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            collectionNameText = itemView.findViewById(R.id.pdfCollectionName);
            collectionDateText = itemView.findViewById(R.id.pdfCollectionDate);
        }
    }
}
