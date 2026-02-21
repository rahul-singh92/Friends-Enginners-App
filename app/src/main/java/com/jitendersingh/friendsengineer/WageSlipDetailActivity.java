package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WageSlipDetailActivity extends BaseActivity {

    private LinearLayout backButton;
    private LinearLayout emptyStateLayout;

    private TextView headingText;
    private TextView txtName, txtFather, txtId;
    private TextView txtPdfPage, txtPdfUrl;
    private TextView btnOpenPdf;

    private FirebaseFirestore firestore;

    private String collectionName;
    private String workerId;

    private String pdfUrl = "";
    private int pdfPage = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wage_slip_detail);

        applyEdgeToEdge(R.id.root_layout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firestore = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.backButton);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        headingText = findViewById(R.id.headingText);

        txtName = findViewById(R.id.txtName);
        txtFather = findViewById(R.id.txtFather);
        txtId = findViewById(R.id.txtId);
        txtPdfPage = findViewById(R.id.txtPdfPage);
        txtPdfUrl = findViewById(R.id.txtPdfUrl);

        btnOpenPdf = findViewById(R.id.btnOpenPdf);

        backButton.setOnClickListener(v -> finish());

        collectionName = getIntent().getStringExtra("collectionName");
        workerId = getIntent().getStringExtra("workerId");

        if (collectionName == null || workerId == null) {
            showEmpty();
            return;
        }

        headingText.setText(collectionName.replace("_", " ").toUpperCase());

        fetchWorkerWage();
    }

    private void fetchWorkerWage() {
        firestore.collection(collectionName)
                .document(workerId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        showEmpty();
                        return;
                    }

                    showData(documentSnapshot);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmpty();
                });
    }

    private void showData(DocumentSnapshot doc) {

        emptyStateLayout.setVisibility(View.GONE);

        Object idObj = doc.get("id");
        String id = idObj != null ? String.valueOf(idObj) : workerId;

        String name = doc.getString("name");
        String fatherName = doc.getString("fatherName");

        pdfUrl = doc.getString("pdfUrl");

        Long pageLong = doc.getLong("pdfPage");
        pdfPage = (pageLong != null) ? pageLong.intValue() : 1;

        txtId.setText(id);
        txtName.setText(name != null ? name : "Unknown");
        txtFather.setText(fatherName != null ? fatherName : "Unknown");

        txtPdfPage.setText(String.valueOf(pdfPage));
        txtPdfUrl.setText(pdfUrl != null && !pdfUrl.isEmpty() ? "Available" : "Not Available");

        btnOpenPdf.setOnClickListener(v -> {
            if (pdfUrl == null || pdfUrl.isEmpty()) {
                Toast.makeText(this, "PDF not available", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Open only mapped page using your existing ViewPdfPageActivity
            Intent intent = new Intent(WageSlipDetailActivity.this, ViewPdfPageActivity.class);
            intent.putExtra("pdfUrl", pdfUrl);
            intent.putExtra("pdfPage", pdfPage);
            intent.putExtra("personName", txtName.getText().toString());
            startActivity(intent);
        });
    }

    private void showEmpty() {
        emptyStateLayout.setVisibility(View.VISIBLE);
    }
}
