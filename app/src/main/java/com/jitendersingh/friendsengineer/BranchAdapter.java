package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.ViewHolder> {

    private List<String> branches;
    private OnBranchClickListener listener;

    public interface OnBranchClickListener {
        void onBranchClick(String branchName);
    }

    public BranchAdapter(List<String> branches, OnBranchClickListener listener) {
        this.branches = branches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_branch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String branchName = branches.get(position);

        holder.branchNameText.setText(branchName);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBranchClick(branchName);
            }
        });
    }

    @Override
    public int getItemCount() {
        return branches.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView branchNameText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            branchNameText = itemView.findViewById(R.id.branchName);
        }
    }
}
