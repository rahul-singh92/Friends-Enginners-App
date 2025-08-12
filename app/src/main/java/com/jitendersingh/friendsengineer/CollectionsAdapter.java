package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CollectionsAdapter extends RecyclerView.Adapter<CollectionsAdapter.ViewHolder> {

    public interface OnCollectionClickListener {
        void onClick(String collectionName);
    }

    private List<String> collections;
    private OnCollectionClickListener listener;

    public CollectionsAdapter(List<String> collections, OnCollectionClickListener listener) {
        this.collections = collections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CollectionsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionsAdapter.ViewHolder holder, int position) {
        String collectionName = collections.get(position);
        holder.textView.setText(collectionName);
        holder.itemView.setTag(collectionName);
    }

    @Override
    public int getItemCount() {
        return collections.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView, OnCollectionClickListener listener) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);

            itemView.setOnClickListener(v -> {
                String collectionName = (String) v.getTag();
                if (listener != null) {
                    listener.onClick(collectionName);
                }
            });
        }
    }
}
