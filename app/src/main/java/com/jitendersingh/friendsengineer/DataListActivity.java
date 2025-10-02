package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

public class DataListActivity extends AppCompatActivity {

    private static final String TAG = "DataListActivity";
    private String collectionName;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private EditText searchEditText;
    private TextView headerSubtitle;
    private TextView recordCount;
    private DataRecordAdapter adapter;
    private List<DataItem> allDataItems = new ArrayList<>();
    private FirebaseFirestore firestore;
    private ListenerRegistration firestoreListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        collectionName = getIntent().getStringExtra("TABLE_NAME");
        if (collectionName == null || collectionName.isEmpty()) {
            Toast.makeText(this, "Invalid collection name", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewData);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);
        searchEditText = findViewById(R.id.searchEditText);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        recordCount = findViewById(R.id.recordCount);

        // Set collection name in header
        headerSubtitle.setText(collectionName.replace("_", " ").toUpperCase());

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DataRecordAdapter(allDataItems, this::onItemClicked, this::onItemLongClicked);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        loadDataFromFirestore();

        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDataFromFirestore() {
        CollectionReference collectionRef = firestore.collection(collectionName);

        firestoreListener = collectionRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e);
                    Toast.makeText(DataListActivity.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (snapshots == null || snapshots.isEmpty()) {
                    allDataItems.clear();
                    adapter.updateData(allDataItems);
                    recyclerView.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                    recordCount.setText("0");
                    return;
                }

                allDataItems.clear();

                for (DocumentSnapshot doc : snapshots) {
                    String id = doc.getId();
                    String name = doc.contains("Name") ? doc.getString("Name") : "N/A";
                    String fatherName = doc.contains("FATHER_S_NAME") ? doc.getString("FATHER_S_NAME") : "N/A";

                    allDataItems.add(new DataItem(id, name, fatherName));
                }

                adapter.updateData(allDataItems);
                recyclerView.setVisibility(View.VISIBLE);
                emptyState.setVisibility(View.GONE);
                recordCount.setText(String.valueOf(allDataItems.size()));
            }
        });
    }

    private void filterData(String query) {
        if (adapter == null) return;

        String lowerQuery = query.toLowerCase();

        List<DataItem> filtered = new ArrayList<>();
        for (DataItem item : allDataItems) {
            String name = (item.getName() != null) ? item.getName().toLowerCase() : "";
            String fatherName = (item.getFatherName() != null) ? item.getFatherName().toLowerCase() : "";

            if (name.contains(lowerQuery) || fatherName.contains(lowerQuery)) {
                filtered.add(item);
            }
        }

        adapter.updateData(filtered);
    }

    private void onItemClicked(DataItem item) {
        Intent intent = new Intent(DataListActivity.this, DataDetailActivity.class);
        intent.putExtra("COLLECTION_NAME", collectionName);
        intent.putExtra("DOCUMENT_ID", item.getDocumentId());
        startActivity(intent);
    }

    private void onItemLongClicked(DataItem item) {
        showDeleteConfirmationDialog(item);
    }

    private void showDeleteConfirmationDialog(DataItem item) {
        // Create dark themed confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Delete Record");
        builder.setMessage("Are you sure you want to delete this record?\n\nName: " + item.getName() + "\nFather's Name: " + item.getFatherName() + "\n\nThis action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> deleteRecord(item.getDocumentId()));
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteRecord(String documentId) {
        firestore.collection(collectionName).document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Record deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting record", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }

    public static class DataItem {
        private final String documentId;
        private final String name;
        private final String fatherName;

        public DataItem(String documentId, String name, String fatherName) {
            this.documentId = documentId;
            this.name = name;
            this.fatherName = fatherName;
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getName() {
            return name;
        }

        public String getFatherName() {
            return fatherName;
        }
    }
}
