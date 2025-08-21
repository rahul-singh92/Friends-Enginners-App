package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TableListActivity extends AppCompatActivity {

    private ListView listView;
    private TextView emptyView;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_list);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Available Data Tables");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listView = findViewById(R.id.table_list_view);
        emptyView = findViewById(R.id.empty_view);

        firestore = FirebaseFirestore.getInstance();

        loadTablesFromFirestore();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String tableName = (String) parent.getItemAtPosition(position);

            Intent intent = new Intent(TableListActivity.this, DataListActivity.class);
            intent.putExtra("TABLE_NAME", tableName);
            startActivity(intent);
        });

        // Long click deletion is omitted because Firestore collections require a different approach
    }

    private void loadTablesFromFirestore() {
        firestore.collection("CollectionsList")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> collectionNames = new ArrayList<>();

                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String collectionName = document.getString("name");
                                if (collectionName != null && !collectionName.isEmpty()) {
                                    collectionNames.add(collectionName);
                                }
                            }
                        }

                        if (collectionNames.isEmpty()) {
                            listView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.VISIBLE);
                        } else {
                            listView.setVisibility(View.VISIBLE);
                            emptyView.setVisibility(View.GONE);

                            ArrayAdapter<String> adapter = new ArrayAdapter<>(TableListActivity.this,
                                    android.R.layout.simple_list_item_1, collectionNames);
                            listView.setAdapter(adapter);
                        }
                    } else {
                        listView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
