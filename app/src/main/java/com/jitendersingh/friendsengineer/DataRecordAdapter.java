package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DataRecordAdapter extends RecyclerView.Adapter<DataRecordAdapter.ViewHolder> {

    private List<DataListActivity.DataItem> dataItems;
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public interface OnItemClickListener {
        void onItemClick(DataListActivity.DataItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(DataListActivity.DataItem item);
    }

    public DataRecordAdapter(List<DataListActivity.DataItem> dataItems,
                             OnItemClickListener clickListener,
                             OnItemLongClickListener longClickListener) {
        this.dataItems = dataItems;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_data_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DataListActivity.DataItem item = dataItems.get(position);

        // Set name
        String name = item.getName() != null ? item.getName() : "N/A";
        holder.nameText.setText(name);

        // Set father's name
        String fatherName = item.getFatherName() != null ? item.getFatherName() : "N/A";
        holder.fatherNameText.setText("Father: " + fatherName);

        // Set initial
        if (name != null && !name.isEmpty() && !name.equals("N/A")) {
            holder.initialText.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            holder.initialText.setText("?");
        }

        // Regular click listener - opens detail view
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item);
            }
        });

        // Long press on entire item - shows delete dialog
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item);
                return true; // Consume the event
            }
            return false;
        });

        // Click on more options button - also shows delete dialog
        holder.moreOptions.setOnClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataItems.size();
    }

    public void updateData(List<DataListActivity.DataItem> newData) {
        this.dataItems = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView fatherNameText;
        TextView initialText;
        ImageView moreOptions;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.recordName);
            fatherNameText = itemView.findViewById(R.id.recordFatherName);
            initialText = itemView.findViewById(R.id.userInitial);
            moreOptions = itemView.findViewById(R.id.moreOptions);
        }
    }
}
