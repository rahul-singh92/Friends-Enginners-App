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
    private String workerId;
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

        fetchWorkerId();
    }

    private void fetchWorkerId() {
        firestore.collection("credentials_worker")
                .whereEqualTo("Username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                    Object idObj = doc.get("id");
                    if (idObj != null) {
                        workerId = String.valueOf(idObj);
                    }

                    if (workerId == null || workerId.trim().isEmpty()) {
                        Toast.makeText(this, "Worker ID not found in database", Toast.LENGTH_LONG).show();
                        showEmpty();
                        return;
                    }

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

                                        firestore.collection("salary_data")
                                                .document(branch)
                                                .collection("salary_months")
                                                .document(month)
                                                .collection("employees")
                                                .document(workerId)
                                                .get()
                                                .addOnSuccessListener(employeeDoc -> {

                                                    if (employeeDoc.exists()) {

                                                        availableCollections.add(
                                                                new WageSlipItem(branch, month)
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

            for(WageSlipItem item : availableCollections){

                displayList.add(
                        item.getBranch() + " - " + item.getMonth()
                );

            }

            adapter = new WageCollectionAdapter(displayList, collectionName -> {

                for(WageSlipItem item : availableCollections){

                    String text =
                            item.getBranch() + " - " + item.getMonth();

                    if(text.equals(collectionName)){

                        Intent intent =
                                new Intent(
                                        WageSlipActivity.this,
                                        WageSlipDetailActivity.class);

                        intent.putExtra(
                                "branch",
                                item.getBranch());

                        intent.putExtra(
                                "month",
                                item.getMonth());

                        intent.putExtra(
                                "workerId",
                                workerId);

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
