package com.jitendersingh.friendsengineer.fragments;

import android.app.AlertDialog;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class PendingRequestFragment extends Fragment {

    private RecyclerView recyclerView;
    private WorkerAdapter adapter;
    private TextView noPendingText;

    private FirebaseFirestore firestore;

    public PendingRequestFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = view.findViewById(R.id.recycler_pending);
        noPendingText = view.findViewById(R.id.text_no_pending);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firestore = FirebaseFirestore.getInstance();

        loadPendingRequestsFromFirestore();
    }

    private void loadPendingRequestsFromFirestore() {
        firestore.collection("Requested_Amount")
                .whereEqualTo("Status", "Pending")
                .orderBy("RequestTime", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Worker> pendingList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String docId = doc.getId();
                        String name = doc.getString("Name");
                        String fatherName = doc.getString("FatherName");
                        String amount = doc.getString("Amount");
                        String reason = doc.getString("Reason");

                        Timestamp ts = doc.getTimestamp("RequestTime");
                        String requestTime = "";
                        if (ts != null) {
                            Date date = ts.toDate();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
                            requestTime = sdf.format(date);
                        }

                        if (name != null && fatherName != null && amount != null && reason != null && !requestTime.isEmpty()) {
                            pendingList.add(new Worker(docId, name, fatherName, amount, reason, requestTime));
                        }
                    }

                    if (pendingList.isEmpty()) {
                        noPendingText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        noPendingText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        adapter = new WorkerAdapter(pendingList);
                        recyclerView.setAdapter(adapter);

                        adapter.setOnItemClickListener((worker, position) -> showRequestOptionsDialog(worker, position));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showRequestOptionsDialog(Worker worker, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Request from " + worker.getName());
        builder.setMessage("Choose an action:");

        builder.setPositiveButton("Accept", null);
        builder.setNegativeButton("Reject", (dialog, which) -> {
            firestore.collection("Requested_Amount")
                    .document(worker.getDocId())
                    .update("Status", "Rejected")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                        removeItemFromList(position);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to reject request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            dialog.dismiss();
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            AlertDialog.Builder inputDialogBuilder = new AlertDialog.Builder(requireContext());
            inputDialogBuilder.setTitle("Enter Accepted Amount");

            final View inputView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_input_amount, null);
            inputDialogBuilder.setView(inputView);

            inputDialogBuilder.setPositiveButton("Submit", (d, w) -> {
                TextView inputField = inputView.findViewById(R.id.accepted_amount);
                String enteredAmount = inputField.getText().toString().trim();
                String finalAmount = enteredAmount.isEmpty() ? worker.getAmount() : enteredAmount;

                Map<String, Object> updates = new HashMap<>();
                updates.put("Status", "Accepted");
                updates.put("Amount", finalAmount);

                firestore.collection("Requested_Amount")
                        .document(worker.getDocId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Request accepted for ₹" + finalAmount, Toast.LENGTH_SHORT).show();
                            removeItemFromList(position);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to accept request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                d.dismiss();
                dialog.dismiss();
            });

            inputDialogBuilder.setNegativeButton("Cancel", (d, w) -> d.dismiss());

            inputDialogBuilder.show();
        });
    }

    private void removeItemFromList(int position) {
        if (adapter != null) {
            adapter.workerList.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, adapter.getItemCount());

            if (adapter.getItemCount() == 0) {
                noPendingText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
    }
}
