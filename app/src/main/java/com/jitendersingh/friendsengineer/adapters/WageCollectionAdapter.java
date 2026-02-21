package com.jitendersingh.friendsengineer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jitendersingh.friendsengineer.R;

import java.util.List;

public class WageCollectionAdapter extends RecyclerView.Adapter<WageCollectionAdapter.ViewHolder> {

    public interface OnCollectionClickListener {
        void onClick(String collectionName);
    }

    private List<String> collections;
    private OnCollectionClickListener listener;

    public WageCollectionAdapter(List<String> collections, OnCollectionClickListener listener) {
        this.collections = collections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wage_collection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String collectionName = collections.get(position);

        // Convert "wage_SCB_January_2030" -> "WAGE SCB JANUARY 2030"
        String displayName = collectionName.replace("_", " ").toUpperCase();

        holder.title.setText(displayName);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(collectionName);
        });
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_collection_name);
        }
    }
}
