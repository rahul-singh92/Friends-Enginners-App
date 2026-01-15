package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ViewWorkerDetailsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private TextView workerCount;
    private FirebaseFirestore firestore;
    private WorkerAdapter adapter;
    private List<DocumentSnapshot> workerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_worker_details);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewWorkers);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);
        workerCount = findViewById(R.id.workerCount);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        workerList = new ArrayList<>();

        adapter = new WorkerAdapter(workerList, this::onWorkerClicked);
        recyclerView.setAdapter(adapter);

        loadWorkerDetailsFromFirestore();
    }

    private void loadWorkerDetailsFromFirestore() {
        firestore.collection("Worker_Detail")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                        workerCount.setText("0");
                        return;
                    }

                    workerList.clear();
                    workerList.addAll(queryDocumentSnapshots.getDocuments());

                    // Update worker count
                    workerCount.setText(String.valueOf(workerList.size()));

                    // Show/hide empty state
                    if (workerList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch worker details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FirestoreError", e.getMessage(), e);
                    recyclerView.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                });
    }

    private void onWorkerClicked(DocumentSnapshot worker) {
        String documentId = worker.getId();

        Intent intent = new Intent(ViewWorkerDetailsActivity.this, WorkerDetailActivity.class);
        intent.putExtra("document_id", documentId);
        startActivity(intent);
    }
}
