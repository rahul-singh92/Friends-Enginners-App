package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jitendersingh.friendsengineer.adapters.WageCollectionAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows every month/year uploaded for the selected department (branch).
 * Tapping a month opens AdminEmployeeListActivity for that branch+month.
 */
public class AdminMonthActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private android.widget.TextView headerTitle;
    private android.widget.TextView headerSubtitle;
    private android.widget.TextView monthCount;

    private WageCollectionAdapter adapter;
    // Raw document IDs (e.g. "MAY_2026"), parallel to what's displayed.
    private final List<String> monthDocIds = new ArrayList<>();
    // Nicely formatted display strings (e.g. "MAY 2026").
    private final List<String> displayList = new ArrayList<>();

    private FirebaseFirestore firestore;
    private String department;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_month);

        applyEdgeToEdge(R.id.root_layout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        recyclerView = findViewById(R.id.recyclerViewMonths);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);
        headerTitle = findViewById(R.id.headerTitle);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        monthCount = findViewById(R.id.monthCount);

        backButton.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        department = getIntent().getStringExtra("department");

        if (department == null || department.trim().isEmpty()) {
            Toast.makeText(this, "Department not specified", Toast.LENGTH_SHORT).show();
            showEmpty();
            return;
        }

        headerTitle.setText(department.replace("_", " "));
        headerSubtitle.setText("Choose a month to view wage slips");

        loadMonths();
    }

    private void loadMonths() {
        firestore.collection("salary_data")
                .document(department)
                .collection("salary_months")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    monthDocIds.clear();
                    displayList.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String docId = doc.getId(); // e.g. "MAY_2026"
                        monthDocIds.add(docId);
                        displayList.add(formatForDisplay(docId));
                    }

                    if (displayList.isEmpty()) {
                        showEmpty();
                        return;
                    }

                    recyclerView.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                    monthCount.setText(String.valueOf(displayList.size()));

                    adapter = new WageCollectionAdapter(displayList, this::onMonthClicked);
                    recyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load months: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmpty();
                });
    }

    /** "MAY_2026" -> "MAY 2026" for a cleaner display. */
    private String formatForDisplay(String docId) {
        return docId.replace("_", " ");
    }

    private void onMonthClicked(String displayedText) {
        // Find the matching raw doc ID for the clicked display text.
        int index = displayList.indexOf(displayedText);
        if (index == -1) return;

        String monthDocId = monthDocIds.get(index);

        Intent intent = new Intent(AdminMonthActivity.this, AdminEmployeeListActivity.class);
        intent.putExtra("department", department);
        intent.putExtra("month", monthDocId);
        startActivity(intent);
    }

    private void showEmpty() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }
}