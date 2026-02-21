package com.jitendersingh.friendsengineer;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;
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
    private List<String> availableCollections = new ArrayList<>();

    private String username;
    private String workerId;

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

                    fetchAllWageCollections();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmpty();
                });
    }

    private void fetchAllWageCollections() {
        firestore.collection("wage_collections")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    List<String> allCollections = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String collectionName = doc.getString("name");
                        if (collectionName != null && !collectionName.trim().isEmpty()) {
                            allCollections.add(collectionName);
                        }
                    }

                    checkWorkerInCollections(allCollections);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmpty();
                });
    }

    private void checkWorkerInCollections(List<String> allCollections) {

        availableCollections.clear();

        if (allCollections.isEmpty()) {
            showEmpty();
            return;
        }

        final int total = allCollections.size();
        final int[] done = {0};

        for (String collectionName : allCollections) {
            firestore.collection(collectionName)
                    .document(workerId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        done[0]++;

                        if (documentSnapshot.exists()) {
                            availableCollections.add(collectionName);
                        }

                        if (done[0] == total) {
                            updateUI();
                        }
                    })
                    .addOnFailureListener(e -> {
                        done[0]++;
                        if (done[0] == total) {
                            updateUI();
                        }
                    });
        }
    }

    private void updateUI() {

        workerCount.setText(String.valueOf(availableCollections.size()));

        if (availableCollections.isEmpty()) {
            showEmpty();
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            adapter = new WageCollectionAdapter(availableCollections, collectionName -> {
                Intent intent = new Intent(WageSlipActivity.this, WageSlipDetailActivity.class);
                intent.putExtra("collectionName", collectionName);
                intent.putExtra("workerId", workerId);
                startActivity(intent);
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
