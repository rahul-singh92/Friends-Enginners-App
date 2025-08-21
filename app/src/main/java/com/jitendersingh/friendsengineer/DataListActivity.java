package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

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
    private ListView listView;
    private TextView emptyView;
    private androidx.appcompat.widget.SearchView searchView;
    private DataAdapter adapter;
    private List<DataItem> allDataItems = new ArrayList<>();
    private FirebaseFirestore firestore;
    private ListenerRegistration firestoreListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        collectionName = getIntent().getStringExtra("TABLE_NAME");
        if (collectionName == null || collectionName.isEmpty()) {
            Toast.makeText(this, "Invalid collection name", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Records");
            getSupportActionBar().setSubtitle(collectionName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = findViewById(R.id.data_list_view);
        emptyView = findViewById(R.id.empty_view);
        searchView = findViewById(R.id.search_view);

        firestore = FirebaseFirestore.getInstance();

        loadDataFromFirestore();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterData(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterData(newText);
                return true;
            }
        });

        // Normal click: You can open detailed view if implemented
        listView.setOnItemClickListener((parent, view, position, id) -> {
            DataItem item = (DataItem) parent.getItemAtPosition(position);
            Intent intent = new Intent(DataListActivity.this, DataDetailActivity.class);
            intent.putExtra("COLLECTION_NAME", collectionName);
            intent.putExtra("DOCUMENT_ID", item.getDocumentId());
            startActivity(intent);
        });

        // Long click: Show deletion dialog for Firestore document
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            DataItem item = (DataItem) parent.getItemAtPosition(position);
            showDeleteConfirmationDialog(item);
            return true;
        });
    }

    private void loadDataFromFirestore() {
        CollectionReference collectionRef = firestore.collection(collectionName);

        // Real-time listener (optional), or use get() for one-time fetch
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
                    adapter = new DataAdapter(DataListActivity.this, allDataItems);
                    listView.setAdapter(adapter);
                    listView.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    return;
                }

                allDataItems.clear();

                for (DocumentSnapshot doc : snapshots) {
                    // Extract fields according to your uploaded data structure
                    // For example, you might commonly have "Name" and "FATHER_S_NAME" fields
                    String id = doc.getId();
                    String name = doc.contains("Name") ? doc.getString("Name") : "N/A";
                    String fatherName = doc.contains("FATHER_S_NAME") ? doc.getString("FATHER_S_NAME") : "N/A";

                    allDataItems.add(new DataItem(id, name, fatherName));
                }

                adapter = new DataAdapter(DataListActivity.this, allDataItems);
                listView.setAdapter(adapter);
                listView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        });
    }

    private void filterData(String query) {
        if (adapter == null) return; // safeguard

        String lowerQuery = query.toLowerCase();

        List<DataItem> filtered = new ArrayList<>();
        for (DataItem item : allDataItems) {
            String name = (item.getName() != null) ? item.getName().toLowerCase() : "";
            String fatherName = (item.getFatherName() != null) ? item.getFatherName().toLowerCase() : "";

            if (name.contains(lowerQuery) || fatherName.contains(lowerQuery)) {
                filtered.add(item);
            }
        }

        adapter.updateData(filtered);  // updateData method should update adapter's list
        adapter.notifyDataSetChanged();
    }




    private void showDeleteConfirmationDialog(DataItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Are you sure you want to delete this record?\n\nName: " + item.getName())
                .setPositiveButton("Delete", (dialog, which) -> deleteRecord(item.getDocumentId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecord(String documentId) {
        firestore.collection(collectionName).document(documentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                    // The listener will update the list automatically
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete record", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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
