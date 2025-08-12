package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RequestedAmountAdapter extends RecyclerView.Adapter<RequestedAmountAdapter.ViewHolder> {

    List<RequestedAmountModel> list;

    public RequestedAmountAdapter(List<RequestedAmountModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public RequestedAmountAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_requested_amount, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestedAmountAdapter.ViewHolder holder, int position) {
        RequestedAmountModel model = list.get(position);
        holder.txtAmount.setText("₹" + model.amount);
        holder.txtReason.setText(model.reason);
        holder.txtDate.setText(model.date);
        holder.txtStatus.setText(model.status);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAmount, txtReason, txtDate, txtStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAmount = itemView.findViewById(R.id.txt_amount);
            txtReason = itemView.findViewById(R.id.txt_reason);
            txtDate = itemView.findViewById(R.id.txt_date);
            txtStatus = itemView.findViewById(R.id.txt_status);
        }
    }
}
