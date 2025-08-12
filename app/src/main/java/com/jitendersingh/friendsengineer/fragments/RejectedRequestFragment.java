package com.jitendersingh.friendsengineer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.jitendersingh.friendsengineer.R;
import com.jitendersingh.friendsengineer.adapters.WorkerAdapter;
import com.jitendersingh.friendsengineer.models.Worker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class RejectedRequestFragment extends Fragment {

    private RecyclerView recyclerView;
    private WorkerAdapter adapter;
    private TextView noRejectedText;
    private FirebaseFirestore firestore;
    private List<Worker> rejectedList;

    public RejectedRequestFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rejected_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.rejected_recycler_view);
        noRejectedText = view.findViewById(R.id.text_no_rejected);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firestore = FirebaseFirestore.getInstance();

        loadRejectedRequestsFromFirestore();
    }

    private void loadRejectedRequestsFromFirestore() {
        firestore.collection("Requested_Amount")
                .whereEqualTo("Status", "Rejected")
                .orderBy("RequestTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    rejectedList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String docId = doc.getId();
                        String name = doc.getString("Name");
                        String fatherName = doc.getString("FatherName");
                        String amount = doc.getString("Amount");
                        String reason = doc.getString("Reason");

                        // Handle Timestamp for RequestTime
                        Timestamp ts = doc.getTimestamp("RequestTime");
                        String requestTime = "";
                        if (ts != null) {
                            Date date = ts.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                            requestTime = sdf.format(date);
                        }

                        if (name != null && fatherName != null && amount != null && reason != null && !requestTime.isEmpty()) {
                            Worker worker = new Worker(docId, name, fatherName, amount, reason, requestTime);
                            worker.setDocId(docId);
                            rejectedList.add(worker);
                        }
                    }

                    if (rejectedList.isEmpty()) {
                        noRejectedText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        noRejectedText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter = new WorkerAdapter(rejectedList);
                        recyclerView.setAdapter(adapter);

                        adapter.setOnItemLongClickListener((worker, position) -> {
                            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                    .setTitle("Delete Rejected Request")
                                    .setMessage("Are you sure you want to delete this rejected request from " + worker.getName() + "?")
                                    .setPositiveButton("Delete", (dialog, which) -> {
                                        deleteRequestFromFirestore(worker, position);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load rejected requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    noRejectedText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    private void deleteRequestFromFirestore(Worker worker, int position) {
        if (worker.getDocId() == null) {
            Toast.makeText(getContext(), "Document ID missing, cannot delete", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("Requested_Amount")
                .document(worker.getDocId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Deleted rejected request from " + worker.getName(), Toast.LENGTH_SHORT).show();
                    rejectedList.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, adapter.getItemCount());

                    if (adapter.getItemCount() == 0) {
                        recyclerView.setVisibility(View.GONE);
                        noRejectedText.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete rejected request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
