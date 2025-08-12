package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewWorkerDetailsActivity extends AppCompatActivity {

    private ListView listView;
    private FirebaseFirestore firestore;
    private ArrayAdapter<String> adapter;
    private List<String> workerList;
    private List<DocumentSnapshot> documentSnapshots; // for click reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_worker_details);

        listView = findViewById(R.id.worker_list_view);
        firestore = FirebaseFirestore.getInstance();
        workerList = new ArrayList<>();
        documentSnapshots = new ArrayList<>();

        loadWorkerDetailsFromFirestore();
    }

    private void loadWorkerDetailsFromFirestore() {
        firestore.collection("Worker_Detail")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No worker details found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("Name");
                        String fatherName = doc.getString("FatherName");
                        String id = doc.getId(); // Firestore document ID

                        workerList.add("Name: " + name + "\nFather's Name: " + fatherName);
                        documentSnapshots.add(doc);
                    }

                    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, workerList);
                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        DocumentSnapshot selectedDoc = documentSnapshots.get(position);
                        String documentId = selectedDoc.getId();

                        Intent intent = new Intent(ViewWorkerDetailsActivity.this, WorkerDetailActivity.class);
                        intent.putExtra("document_id", documentId);
                        startActivity(intent);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch worker details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FirestoreError", e.getMessage(), e);
                });
    }
}
