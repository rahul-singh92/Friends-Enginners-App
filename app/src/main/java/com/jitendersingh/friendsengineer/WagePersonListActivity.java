package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WagePersonListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PersonAdapter adapter;
    private List<PersonData> persons;
    private FirebaseFirestore firestore;
    private String collectionName;
    private TextView headerTitle;
    private TextView workerCount;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wage_person_list);

        // Initialize views
        headerTitle = findViewById(R.id.headerTitle);
        workerCount = findViewById(R.id.workerCount);
        emptyState = findViewById(R.id.emptyState);
        recyclerView = findViewById(R.id.recyclerViewPersons);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Add divider between items
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.divider_line));
        recyclerView.addItemDecoration(divider);

        persons = new ArrayList<>();
        adapter = new PersonAdapter(persons, this::onPersonClicked);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        collectionName = getIntent().getStringExtra("collectionName");
        if (collectionName == null) {
            Toast.makeText(this, "No collection selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set header title with formatted name
        String displayName = formatCollectionName(collectionName);
        headerTitle.setText(displayName);

        loadPersons();
    }

    private String formatCollectionName(String name) {
        // Remove "wage_collection_" prefix if exists
        if (name.startsWith("wage_collection_")) {
            name = name.substring(16);
        }
        // Replace underscores with spaces and capitalize
        return name.replace("_", " ").toUpperCase();
    }

    private void loadPersons() {
        firestore.collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    persons.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (doc.getId().equals("_metadata")) continue;

                        String id = doc.getId();
                        String name = doc.getString("name");
                        String fatherName = doc.getString("fatherName");
                        String pdfUrl = doc.getString("pdfUrl");
                        Long pageLong = doc.getLong("pdfPage");
                        int pdfPage = pageLong != null ? pageLong.intValue() : 1;

                        persons.add(new PersonData(id, name, fatherName, pdfUrl, pdfPage));
                    }

                    // Update worker count
                    workerCount.setText(String.valueOf(persons.size()));

                    // Show/hide empty state
                    if (persons.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load persons", Toast.LENGTH_SHORT).show();
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
    }

    private void onPersonClicked(PersonData person) {
        Intent intent = new Intent(this, ViewPdfPageActivity.class);
        intent.putExtra("pdfUrl", person.getPdfUrl());
        intent.putExtra("pdfPage", person.getPdfPage());
        intent.putExtra("personName", person.getName());
        startActivity(intent);
    }

    public static class PersonData {
        private String id, name, fatherName, pdfUrl;
        private int pdfPage;

        public PersonData(String id, String name, String fatherName, String pdfUrl, int pdfPage) {
            this.id = id;
            this.name = name;
            this.fatherName = fatherName;
            this.pdfUrl = pdfUrl;
            this.pdfPage = pdfPage;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getFatherName() { return fatherName; }
        public String getPdfUrl() { return pdfUrl; }
        public int getPdfPage() { return pdfPage; }
    }
}
