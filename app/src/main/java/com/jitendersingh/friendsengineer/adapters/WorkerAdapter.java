package com.jitendersingh.friendsengineer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jitendersingh.friendsengineer.R;
import com.jitendersingh.friendsengineer.models.Worker;

import java.util.List;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {

    public final List<Worker> workerList;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;

    // Existing interface for click callbacks
    public interface OnItemClickListener {
        void onItemClick(Worker worker, int position);
    }

    // New interface for long click callbacks
    public interface OnItemLongClickListener {
        void onItemLongClick(Worker worker, int position);
    }

    // Setters for listeners
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public WorkerAdapter(List<Worker> workerList) {
        this.workerList = workerList;
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_worker_request, parent, false);
        return new WorkerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        Worker worker = workerList.get(position);
        holder.nameText.setText(worker.getName());
        holder.fatherNameText.setText(worker.getFatherName());
        holder.amountText.setText("₹" + worker.getAmount());
        holder.reasonText.setText(worker.getReason());
        holder.timeText.setText(worker.getRequestTime());

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(worker, position);
            }
        });

        // Long click listener
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(worker, position);
                return true; // Consume event
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return workerList.size();
    }

    static class WorkerViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, fatherNameText, amountText, reasonText, timeText;

        public WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.worker_name);
            fatherNameText = itemView.findViewById(R.id.worker_father_name);
            amountText = itemView.findViewById(R.id.worker_amount);
            reasonText = itemView.findViewById(R.id.worker_reason);
            timeText = itemView.findViewById(R.id.worker_time);
        }
    }
}