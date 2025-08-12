package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CredentialsDetailsActivity extends AppCompatActivity {

    private TextView headerTextView;
    private ListView listViewDetails;
    private ArrayAdapter<String> adapter;
    private List<String> detailsList;
    private Map<String, Map<String, Object>> lastFetchedDocuments = new HashMap<>();
    private List<String> documentIds;

    private FirebaseFirestore firestore;
    private String collectionName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credentials_details);

        headerTextView = findViewById(R.id.headerTextView);
        listViewDetails = findViewById(R.id.listViewDetails);

        firestore = FirebaseFirestore.getInstance();
        collectionName = getIntent().getStringExtra("collection_name");

        if (collectionName == null || collectionName.isEmpty()) {
            Toast.makeText(this, "Collection name not provided", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        headerTextView.setText("Credentials Details");

        detailsList = new ArrayList<>();
        documentIds = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, detailsList);
        listViewDetails.setAdapter(adapter);

        loadCredentials();

        // Long click listener
        listViewDetails.setOnItemLongClickListener((parent, view, position, id) -> {
            showActionDialog(position);
            return true;
        });
    }

    public void loadCredentials() {
        firestore.collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No data found in the selected collection", Toast.LENGTH_LONG).show();
                        return;
                    }

                    detailsList.clear();
                    documentIds.clear();
                    lastFetchedDocuments.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = document.getData();

                        // Store document data for later use in edit
                        lastFetchedDocuments.put(document.getId(), data);

                        String username = (String) data.get("Username");
                        String password = (String) data.get("Password");
                        String name = (String) data.get("Name");
                        String fatherName = (String) data.get("FATHER_S_NAME");

                        if (username == null) username = "N/A";
                        if (password == null) password = "N/A";
                        if (name == null) name = "N/A";
                        if (fatherName == null) fatherName = "N/A";

                        String displayText = "Username: " + username +
                                "\nPassword: " + password +
                                "\nName: " + name +
                                "\nFather's Name: " + fatherName;

                        detailsList.add(displayText);
                        documentIds.add(document.getId()); // Store Firestore document ID
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreFetch", "Error fetching documents", e);
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void showActionDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Action");
        builder.setItems(new CharSequence[]{"Edit", "Delete"}, (dialog, which) -> {
            if (which == 0) {
                // Edit
                if (position >= 0 && position < documentIds.size()) {
                    String docId = documentIds.get(position);
                    Map<String, Object> docData = lastFetchedDocuments.get(docId);

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
                }
            } else if (which == 1) {
                // Delete
                deleteDocument(position);
            }
        });
        builder.show();
    }

    private void deleteDocument(int position) {
        if (position < 0 || position >= documentIds.size()) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            return;
        }

        String docId = documentIds.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firestore.collection(collectionName)
                            .document(docId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                detailsList.remove(position);
                                documentIds.remove(position);
                                lastFetchedDocuments.remove(docId);
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
