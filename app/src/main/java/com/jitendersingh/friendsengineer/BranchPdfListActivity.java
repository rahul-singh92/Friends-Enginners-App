package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BranchPdfListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private TextView headerSubtitle;
    private TextView scheduleCount;
    private TextView emptyMessage;
    private PdfAdapter adapter;
    private List<PdfModel> pdfList;
    private String branchName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_pdf_list);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar for modern look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_pdf_list);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        scheduleCount = findViewById(R.id.scheduleCount);
        emptyMessage = findViewById(R.id.emptyMessage);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        pdfList = new ArrayList<>();
        adapter = new PdfAdapter(pdfList, this);
        recyclerView.setAdapter(adapter);

        branchName = getIntent().getStringExtra("branchName");

        if (branchName != null) {
            // Set branch name in header
            headerSubtitle.setText(branchName);
            emptyMessage.setText("No schedules for " + branchName + " yet");

            fetchPdfsForBranch(branchName);
        } else {
            Toast.makeText(this, "Branch not selected", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchPdfsForBranch(String branch) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Schedules")
                .whereEqualTo("spinnerOption", branch)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    pdfList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String pdfUrl = doc.getString("pdfUrl");
                        String startingDate = doc.getString("startDate");
                        String endDate = doc.getString("endDate");
                        String docId = doc.getId();

                        if (pdfUrl != null) {
                            PdfModel pdf = new PdfModel(docId, pdfUrl, startingDate, endDate);
                            pdfList.add(pdf);
                        }
                    }

                    // Update schedule count
                    scheduleCount.setText(String.valueOf(pdfList.size()));

                    // Show/hide empty state
                    if (pdfList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                    // Show empty state on error
                    recyclerView.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                    scheduleCount.setText("0");
                });
    }
}
