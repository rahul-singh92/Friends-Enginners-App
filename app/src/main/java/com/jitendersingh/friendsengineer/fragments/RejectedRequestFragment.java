package com.jitendersingh.friendsengineer.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import java.util.List;

public class RejectedRequestFragment extends Fragment implements RefreshableFragment {

    private RecyclerView recyclerView;
    private WorkerAdapter adapter;
    private LinearLayout emptyStateLayout;
    private FirebaseFirestore firestore;

    private List<Worker> originalList = new ArrayList<>();
    private List<Worker> filteredList = new ArrayList<>();

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
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        firestore = FirebaseFirestore.getInstance();

        loadRejectedRequestsFromFirestore();
    }

    private void loadRejectedRequestsFromFirestore() {
        firestore.collection("Requested_Amount")
                .whereEqualTo("Status", "Rejected")
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
                            requestTime = String.valueOf(ts.toDate().getTime());
                        }

                        if (name != null && fatherName != null && amount != null && reason != null) {
                            Worker worker = new Worker(docId, name, fatherName, amount, reason, requestTime);
                            worker.setDocId(docId);
                            originalList.add(worker);
                        }
                    }

                    applySearchAndFilter();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load rejected requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
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

            adapter.setOnItemLongClickListener((worker, position) -> {
                showDeleteDialog(worker, position);
            });
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

    private void showDeleteDialog(Worker worker, int position) {

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.DarkAlertDialog);
        builder.setTitle("Delete Rejected Request");
        builder.setMessage("Are you sure you want to delete this rejected request?\n\nName: "
                + worker.getName() + "\nAmount: ₹" + worker.getAmount());

        builder.setPositiveButton("Delete", (dialog, which) -> {
            deleteRequestFromFirestore(worker, position);
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
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
                    Toast.makeText(getContext(), "Request deleted successfully", Toast.LENGTH_SHORT).show();
                    loadRejectedRequestsFromFirestore();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
