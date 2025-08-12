package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BranchPdfListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    PdfAdapter adapter;
    List<PdfModel> pdfList;

    String branchName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_pdf_list);

        recyclerView = findViewById(R.id.recycler_pdf_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        pdfList = new ArrayList<>();
        adapter = new PdfAdapter(pdfList, this);
        recyclerView.setAdapter(adapter);

        branchName = getIntent().getStringExtra("branchName");

        if (branchName != null) {
            fetchPdfsForBranch(branchName);
        } else {
            Toast.makeText(this, "Branch not selected", Toast.LENGTH_SHORT).show();
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

                    if (pdfList.isEmpty()) {
                        Toast.makeText(this, "No schedules found for " + branch, Toast.LENGTH_SHORT).show();
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
