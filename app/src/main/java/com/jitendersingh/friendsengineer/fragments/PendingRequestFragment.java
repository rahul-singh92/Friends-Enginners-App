package com.jitendersingh.friendsengineer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jitendersingh.friendsengineer.AdvanceRequestActivity;
import com.jitendersingh.friendsengineer.R;
import com.jitendersingh.friendsengineer.RefreshableFragment;
import com.jitendersingh.friendsengineer.adapters.WorkerAdapter;
import com.jitendersingh.friendsengineer.models.Worker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingRequestFragment extends Fragment implements RefreshableFragment {

    private RecyclerView recyclerView;
    private WorkerAdapter adapter;
    private LinearLayout emptyStateLayout;

    private FirebaseFirestore firestore;

    private List<Worker> originalList = new ArrayList<>();
    private List<Worker> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending_request, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        recyclerView = view.findViewById(R.id.recycler_pending);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        firestore = FirebaseFirestore.getInstance();

        loadPendingRequestsFromFirestore();
    }

    private void loadPendingRequestsFromFirestore() {
        firestore.collection("Requested_Amount")
                .whereEqualTo("Status", "Pending")
                .orderBy("RequestTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    originalList.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        String docId = doc.getId();
                        String name = doc.getString("Name");
                        String fatherName = doc.getString("FatherName");
                        String amount = doc.getString("Amount");
                        String reason = doc.getString("Reason");

                        Timestamp ts = doc.getTimestamp("RequestTime");
                        String requestTime = "";

                        if (ts != null) {
                            requestTime = String.valueOf(ts.toDate().getTime()); // millis string
                        }

                        if (name != null && fatherName != null && amount != null && reason != null) {
                            originalList.add(new Worker(docId, name, fatherName, amount, reason, requestTime));
                        }
                    }

                    applySearchAndFilter();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void applySearchAndFilter() {

        String searchText = AdvanceRequestActivity.currentSearchText;
        String filterType = AdvanceRequestActivity.currentFilterType;

        filteredList.clear();

        for (Worker worker : originalList) {

            String name = worker.getName() != null ? worker.getName().toLowerCase() : "";
            String father = worker.getFatherName() != null ? worker.getFatherName().toLowerCase() : "";

            boolean matchesSearch = searchText.isEmpty()
                    || name.contains(searchText)
                    || father.contains(searchText);

            if (!matchesSearch) continue;

            if (!filterType.equals("All")) {
                if (!isWithinFilter(worker.getRequestTime(), filterType)) continue;
            }

            filteredList.add(worker);
        }

        if (filteredList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            adapter = new WorkerAdapter(filteredList);
            recyclerView.setAdapter(adapter);

            adapter.setOnItemClickListener((worker, position) -> showRequestOptionsDialog(worker, position));
        }
    }

    private boolean isWithinFilter(String requestTime, String filterType) {

        if (requestTime == null || requestTime.isEmpty()) return false;

        long millis;
        try {
            millis = Long.parseLong(requestTime);
        } catch (Exception e) {
            return false;
        }

        Date requestDate = new Date(millis);

        Calendar recordCal = Calendar.getInstance();
        recordCal.setTime(requestDate);

        Calendar now = Calendar.getInstance();

        switch (filterType) {
            case "Day":
                return recordCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && recordCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

            case "Week":
                return recordCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && recordCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR);

            case "Month":
                return recordCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && recordCal.get(Calendar.MONTH) == now.get(Calendar.MONTH);

            case "Year":
                return recordCal.get(Calendar.YEAR) == now.get(Calendar.YEAR);
        }

        return true;
    }

    private void showRequestOptionsDialog(Worker worker, int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DarkAlertDialog);
        builder.setTitle("Request from " + worker.getName());
        builder.setMessage("Amount: ₹" + worker.getAmount() + "\nReason: " + worker.getReason());

        builder.setPositiveButton("Accept", null);
        builder.setNegativeButton("Reject", (dialog, which) -> {
            firestore.collection("Requested_Amount")
                    .document(worker.getDocId())
                    .update("Status", "Rejected")
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                        loadPendingRequestsFromFirestore();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

            dialog.dismiss();
        });

        builder.setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

            AlertDialog.Builder inputDialogBuilder = new AlertDialog.Builder(requireContext(), R.style.DarkAlertDialog);
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
                            Toast.makeText(getContext(), "Accepted ₹" + finalAmount, Toast.LENGTH_SHORT).show();
                            loadPendingRequestsFromFirestore();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

                d.dismiss();
                dialog.dismiss();
            });

            inputDialogBuilder.setNegativeButton("Cancel", (d, w) -> d.dismiss());

            AlertDialog inputDialog = inputDialogBuilder.create();
            inputDialog.show();
        });
    }
}
