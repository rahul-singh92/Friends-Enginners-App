package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class CredentialsTablesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private CredentialTableAdapter adapter;
    private ArrayList<String> tablesList;

    private final String[] expectedCollections = {"credentials_admin", "credentials_worker"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credentials_tables);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar for modern look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewCredentials);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tablesList = new ArrayList<>();
        adapter = new CredentialTableAdapter(tablesList, this::onTableClicked);
        recyclerView.setAdapter(adapter);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Check existence of expected collections
        for (String collectionName : expectedCollections) {
            firestore.collection(collectionName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            tablesList.add(collectionName);

                            // Update UI
                            if (tablesList.isEmpty()) {
                                recyclerView.setVisibility(View.GONE);
                                emptyState.setVisibility(View.VISIBLE);
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                                emptyState.setVisibility(View.GONE);
                            }

                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FirestoreCheck", "Error checking collection: " + collectionName, e);
                    });
        }
    }

    private void onTableClicked(String collectionName) {
        Intent intent = new Intent(CredentialsTablesActivity.this, CredentialsDetailsActivity.class);
        intent.putExtra("collection_name", collectionName);
        startActivity(intent);
    }
}
