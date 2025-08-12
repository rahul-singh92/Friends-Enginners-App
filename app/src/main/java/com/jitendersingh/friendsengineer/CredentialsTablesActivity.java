package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class CredentialsTablesActivity extends AppCompatActivity {

    private ListView listViewTables;
    private ArrayList<String> tablesList;
    private ArrayAdapter<String> adapter;

    private final String[] expectedCollections = {"credentials_admin", "credentials_worker"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credentials_tables);

        listViewTables = findViewById(R.id.listViewTables);
        tablesList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tablesList);
        listViewTables.setAdapter(adapter);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Check existence of expected collections
        for (String collectionName : expectedCollections) {
            firestore.collection(collectionName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            tablesList.add(collectionName);
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FirestoreCheck", "Error checking collection: " + collectionName, e));
        }

        listViewTables.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCollection = tablesList.get(position);
            Intent intent = new Intent(CredentialsTablesActivity.this, CredentialsDetailsActivity.class);
            intent.putExtra("collection_name", selectedCollection);  // note: use "collection_name" instead of "table_name"
            startActivity(intent);
        });
    }
}
