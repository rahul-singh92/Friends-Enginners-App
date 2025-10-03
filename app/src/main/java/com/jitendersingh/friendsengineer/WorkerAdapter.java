package com.jitendersingh.friendsengineer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.ViewHolder> {

    private List<DocumentSnapshot> workers;
    private OnWorkerClickListener listener;

    public interface OnWorkerClickListener {
        void onWorkerClick(DocumentSnapshot worker);
    }

    public WorkerAdapter(List<DocumentSnapshot> workers, OnWorkerClickListener listener) {
        this.workers = workers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_worker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot worker = workers.get(position);

        String name = worker.getString("Name");
        String fatherName = worker.getString("FatherName");
        String department = worker.getString("Department");
        String imageUrl = worker.getString("ImageURL");

        holder.nameText.setText(name != null ? name : "N/A");
        holder.fatherNameText.setText("Father: " + (fatherName != null ? fatherName : "N/A"));

        if (department != null && !department.isEmpty()) {
            holder.departmentText.setText(department);
            holder.departmentText.setVisibility(View.VISIBLE);
        } else {
            holder.departmentText.setVisibility(View.GONE);
        }

        // Load image with Glide
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .circleCrop()
                    .into(holder.avatarImage);
        } else {
            holder.avatarImage.setImageResource(R.drawable.baseline_person_24);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWorkerClick(worker);
            }
        });
    }

    @Override
    public int getItemCount() {
        return workers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView fatherNameText;
        TextView departmentText;
        ImageView avatarImage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.workerName);
            fatherNameText = itemView.findViewById(R.id.workerFatherName);
            departmentText = itemView.findViewById(R.id.workerDepartment);
            avatarImage = itemView.findViewById(R.id.workerAvatar);
        }
    }
}
