package com.jitendersingh.friendsengineer.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jitendersingh.friendsengineer.R;
import com.jitendersingh.friendsengineer.models.WorkerRequest;

import java.util.List;

public class WorkerRequestAdapter extends RecyclerView.Adapter<WorkerRequestAdapter.ViewHolder> {

    private List<WorkerRequest> requestList;

    public WorkerRequestAdapter(List<WorkerRequest> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wokers_worker_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkerRequest request = requestList.get(position);
        holder.amount.setText("₹" + request.getAmount());
        holder.date.setText(request.getDate());
        holder.status.setText(request.getStatus());

        if ("Accepted".equalsIgnoreCase(request.getStatus())) {
            holder.status.setTextColor(Color.parseColor("#4CAF50"));
        } else if ("Rejected".equalsIgnoreCase(request.getStatus())) {
            holder.status.setTextColor(Color.parseColor("#F44336"));
        } else {
            holder.status.setTextColor(Color.parseColor("#FF9800"));
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amount, date, status;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            amount = itemView.findViewById(R.id.text_amount);
            date = itemView.findViewById(R.id.text_date);
            status = itemView.findViewById(R.id.text_status);
        }
    }
}
