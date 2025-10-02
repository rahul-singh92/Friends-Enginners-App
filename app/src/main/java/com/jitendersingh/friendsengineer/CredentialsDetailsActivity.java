package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CredentialsDetailsActivity extends AppCompatActivity {

    private TextView headerTextView;
    private TextView headerSubtitle;
    private TextView userCount;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private CredentialUserAdapter adapter;
    private Map<String, Map<String, Object>> lastFetchedDocuments = new HashMap<>();
    private List<String> documentIds;

    private FirebaseFirestore firestore;
    private String collectionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credentials_details);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        headerTextView = findViewById(R.id.headerTextView);
        headerSubtitle = findViewById(R.id.headerSubtitle);
        userCount = findViewById(R.id.userCount);
        recyclerView = findViewById(R.id.recyclerViewCredentials);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();
        collectionName = getIntent().getStringExtra("collection_name");

        if (collectionName == null || collectionName.isEmpty()) {
            Toast.makeText(this, "Collection name not provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Set header based on collection type
        if (collectionName.contains("admin")) {
            headerTextView.setText("Admin Credentials");
            headerSubtitle.setText("Manage administrator accounts");
        } else if (collectionName.contains("worker")) {
            headerTextView.setText("Worker Credentials");
            headerSubtitle.setText("Manage worker accounts");
        } else {
            headerTextView.setText("Credentials Details");
            headerSubtitle.setText("Manage user credentials");
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        documentIds = new ArrayList<>();
        adapter = new CredentialUserAdapter(documentIds, lastFetchedDocuments, this::showActionDialog);
        recyclerView.setAdapter(adapter);

        loadCredentials();
    }

    public void loadCredentials() {
        firestore.collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                        userCount.setText("0");
                        return;
                    }

                    documentIds.clear();
                    lastFetchedDocuments.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = document.getData();
                        lastFetchedDocuments.put(document.getId(), data);
                        documentIds.add(document.getId());
                    }

                    // Update user count
                    userCount.setText(String.valueOf(documentIds.size()));

                    // Show/hide empty state
                    if (documentIds.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreFetch", "Error fetching documents", e);
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    recyclerView.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                });
    }

    private void showActionDialog(int position, String docId, Map<String, Object> docData) {
        // Create dark themed action dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Choose Action");
        builder.setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
            if (which == 0) {
                // Edit
                if (docData != null) {
                    String username = (String) docData.get("Username");
                    String password = (String) docData.get("Password");
                    String name = (String) docData.get("Name");
                    String fatherName = (String) docData.get("FATHER_S_NAME");

                    EditCredentialBottomSheet editSheet = EditCredentialBottomSheet.newInstance(
                            collectionName, docId, username, password, name, fatherName);
                    editSheet.show(getSupportFragmentManager(), "edit_credential");
                } else {
                    Toast.makeText(this, "Document data not found for editing", Toast.LENGTH_SHORT).show();
                }
            } else if (which == 1) {
                // Delete
                deleteDocument(position, docId);
            }
        });

        AlertDialog dialog = builder.create();

        // Additional styling after dialog is created
        dialog.setOnShowListener(dialogInterface -> {
            // You can further customize the dialog here if needed
        });

        dialog.show();
    }

    private void deleteDocument(int position, String docId) {
        // Create dark themed confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkAlertDialog);
        builder.setTitle("Confirm Delete");
        builder.setMessage("Are you sure you want to delete this user?\n\nThis action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            firestore.collection(collectionName)
                    .document(docId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                        documentIds.remove(position);
                        lastFetchedDocuments.remove(docId);

                        // Update user count
                        userCount.setText(String.valueOf(documentIds.size()));

                        adapter.notifyDataSetChanged();

                        // Show empty state if no items left
                        if (documentIds.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyState.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("DeleteUser", "Error deleting user", e);
                    });
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
