package com.jitendersingh.friendsengineer.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jitendersingh.friendsengineer.R;
import com.jitendersingh.friendsengineer.models.Worker;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TotalAdvanceAdapter extends RecyclerView.Adapter<TotalAdvanceAdapter.ViewHolder> {

    private List<Worker> workerList;

    public TotalAdvanceAdapter(List<Worker> workerList) {
        this.workerList = workerList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_total_advance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Worker worker = workerList.get(position);
        holder.name.setText(worker.getName());
        holder.fatherName.setText(worker.getFatherName());

        // Format amount with commas
        try {
            int amount = Integer.parseInt(worker.getAmount());
            holder.totalAdvance.setText("₹" + NumberFormat.getNumberInstance(Locale.US).format(amount));
        } catch (NumberFormatException e) {
            holder.totalAdvance.setText("₹0");
        }
    }

    @Override
    public int getItemCount() {
        return workerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, fatherName, totalAdvance;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_name);
            fatherName = itemView.findViewById(R.id.text_father_name);
            totalAdvance = itemView.findViewById(R.id.text_total_advance);
        }
    }
}
