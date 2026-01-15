package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TableListActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private LinearLayout backButton;
    private FirebaseFirestore firestore;
    private TableAdapter adapter;
    private List<String> tableNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_list);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar for modern look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerViewTables);
        emptyState = findViewById(R.id.emptyState);
        backButton = findViewById(R.id.backButton);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tableNames = new ArrayList<>();
        adapter = new TableAdapter(tableNames, this::onTableClicked);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        loadTablesFromFirestore();
    }

    private void loadTablesFromFirestore() {
        firestore.collection("CollectionsList")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        tableNames.clear();

                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String collectionName = document.getString("name");
                                if (collectionName != null && !collectionName.isEmpty()) {
                                    tableNames.add(collectionName);
                                }
                            }
                        }

                        // Show/hide empty state
                        if (tableNames.isEmpty()) {
                            recyclerView.setVisibility(View.GONE);
                            emptyState.setVisibility(View.VISIBLE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyState.setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChanged();
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void onTableClicked(String tableName) {
        Intent intent = new Intent(TableListActivity.this, DataListActivity.class);
        intent.putExtra("TABLE_NAME", tableName);
        startActivity(intent);
    }
}
