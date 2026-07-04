package com.jitendersingh.friendsengineer;

import com.jitendersingh.friendsengineer.models.WageSlipItem;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.jitendersingh.friendsengineer.adapters.WageCollectionAdapter;

import java.util.ArrayList;
import java.util.List;

public class WageSlipActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout backButton;
    private TextView workerCount;

    private FirebaseFirestore firestore;

    private WageCollectionAdapter adapter;
    private List<WageSlipItem> availableCollections = new ArrayList<>();

    private String username;

    // Real employee identity used to match salary_data records.
    // NOTE: credentials_worker's "id" field (1, 2, 3...) has NO relation
    // to the punchingNo used as the employee document ID in salary_data.
    // We match by name + fatherName instead (normalized: trimmed + lowercased).
    private String workerNameSearch;
    private String workerFatherNameSearch;

    // Display versions (as typed in credentials_worker), used for showing
    // the worker's real name/father name if needed elsewhere.
    private String workerDisplayName;
    private String workerDisplayFatherName;

    private int pendingOperations = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wage_slip);

        applyEdgeToEdge(R.id.root_layout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        recyclerView = findViewById(R.id.recycler_wage_collections);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        backButton = findViewById(R.id.backButton);
        workerCount = findViewById(R.id.workerCount);

        backButton.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();

        username = getIntent().getStringExtra("username");

        fetchWorkerIdentity();
    }

    /**
     * Looks up the logged-in worker's name + fatherName from
     * credentials_worker. These are what we match against in
     * salary_data, NOT the sequential "id" field.
     */
    private void fetchWorkerIdentity() {
        firestore.collection("credentials_worker")
                .whereEqualTo("Username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                    workerDisplayName = doc.getString("Name");
                    workerDisplayFatherName = doc.getString("FATHER_S_NAME");

                    if (workerDisplayName == null || workerDisplayName.trim().isEmpty()
                            || workerDisplayFatherName == null || workerDisplayFatherName.trim().isEmpty()) {
                        Toast.makeText(this,
                                "Name / Father Name not set for this worker in database",
                                Toast.LENGTH_LONG).show();
                        showEmpty();
                        return;
                    }

                    workerNameSearch = workerDisplayName.trim().toLowerCase();
                    workerFatherNameSearch = workerDisplayFatherName.trim().toLowerCase();

                    fetchSalarySlips();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmpty();
                });
    }

    private void fetchSalarySlips() {

        availableCollections.clear();
        pendingOperations = 1;

        firestore.collection("salary_data")
                .get()
                .addOnSuccessListener(branchSnapshot -> {

                    if (branchSnapshot.isEmpty()) {
                        pendingOperations--;
                        checkFinished();
                        return;
                    }

                    for (DocumentSnapshot branchDoc : branchSnapshot.getDocuments()) {

                        String branch = branchDoc.getId();

                        pendingOperations++;

                        firestore.collection("salary_data")
                                .document(branch)
                                .collection("salary_months")
                                .get()
                                .addOnSuccessListener(monthSnapshot -> {

                                    for (DocumentSnapshot monthDoc : monthSnapshot.getDocuments()) {

                                        String month = monthDoc.getId();

                                        pendingOperations++;

                                        // Match by normalized name + fatherName
                                        // instead of a direct document ID lookup.
                                        firestore.collection("salary_data")
                                                .document(branch)
                                                .collection("salary_months")
                                                .document(month)
                                                .collection("employees")
                                                .whereEqualTo("nameSearch", workerNameSearch)
                                                .whereEqualTo("fatherNameSearch", workerFatherNameSearch)
                                                .get()
                                                .addOnSuccessListener(employeeQuery -> {

                                                    if (!employeeQuery.isEmpty()) {

                                                        // Use the first match. If there are
                                                        // multiple employees with the exact
                                                        // same name + father name in the same
                                                        // month, this picks one arbitrarily —
                                                        // worth adding a tie-breaker (e.g. DOB)
                                                        // if that scenario is possible for you.
                                                        DocumentSnapshot employeeDoc =
                                                                employeeQuery.getDocuments().get(0);

                                                        String realWorkerId = employeeDoc.getId();

                                                        availableCollections.add(
                                                                new WageSlipItem(branch, month, realWorkerId)
                                                        );
                                                    }

                                                    pendingOperations--;
                                                    checkFinished();
                                                })
                                                .addOnFailureListener(e -> {
                                                    pendingOperations--;
                                                    checkFinished();
                                                });
                                    }

                                    pendingOperations--;
                                    checkFinished();

                                })
                                .addOnFailureListener(e -> {
                                    pendingOperations--;
                                    checkFinished();
                                });
                    }

                    pendingOperations--;
                    checkFinished();

                })
                .addOnFailureListener(e -> {
                    pendingOperations--;
                    checkFinished();
                });
    }

    private void checkFinished() {
        if (pendingOperations == 0) {
            updateUI();
        }
    }

    private void updateUI() {

        workerCount.setText(String.valueOf(availableCollections.size()));

        if (availableCollections.isEmpty()) {
            showEmpty();
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            List<String> displayList = new ArrayList<>();

            for (WageSlipItem item : availableCollections) {
                displayList.add(item.getBranch() + " - " + item.getMonth());
            }

            adapter = new WageCollectionAdapter(displayList, collectionName -> {

                for (WageSlipItem item : availableCollections) {

                    String text = item.getBranch() + " - " + item.getMonth();

                    if (text.equals(collectionName)) {

                        Intent intent = new Intent(
                                WageSlipActivity.this,
                                WageSlipDetailActivity.class);

                        intent.putExtra("branch", item.getBranch());
                        intent.putExtra("month", item.getMonth());

                        // Pass the REAL employee document ID (punchingNo),
                        // resolved via name+fatherName match above — not
                        // the credentials_worker sequential id.
                        intent.putExtra("workerId", item.getWorkerId());

                        startActivity(intent);
                        break;
                    }
                }
            });

            recyclerView.setAdapter(adapter);
        }
    }

    private void showEmpty() {
        emptyStateLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        workerCount.setText("0");
    }
}