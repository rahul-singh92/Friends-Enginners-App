package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WageCollectionsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CollectionsAdapter adapter;
    private List<String> wageCollections;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wage_collections);

        recyclerView = findViewById(R.id.recyclerViewCollections);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        wageCollections = new ArrayList<>();
        adapter = new CollectionsAdapter(wageCollections, this::onCollectionClicked);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        loadWageCollections();
    }

    private void loadWageCollections() {
        firestore.collection("wage_collections")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    wageCollections.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        if (name != null) wageCollections.add(name);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load collections", Toast.LENGTH_SHORT).show());
    }

    private void onCollectionClicked(String collectionName) {
        Intent intent = new Intent(this, WagePersonListActivity.class);
        intent.putExtra("collectionName", collectionName);
        startActivity(intent);
    }
}
