package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jitendersingh.friendsengineer.adapters.TotalAdvanceAdapter;
import com.jitendersingh.friendsengineer.models.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotalAdvanceActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout backButton;
    private TextView workerCount;
    private TotalAdvanceAdapter adapter;
    private List<Worker> totalAdvanceList;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_advance);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_total_advance);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        backButton = findViewById(R.id.backButton);
        workerCount = findViewById(R.id.workerCount);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        firestore = FirebaseFirestore.getInstance();

        loadTotalAdvance();
    }

    private void loadTotalAdvance() {
        firestore.collection("Requested_Amount")
                .whereEqualTo("Status", "Accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                        recyclerView.setVisibility(RecyclerView.GONE);
                        workerCount.setText("0");
                        return;
                    }

                    Map<String, Integer> advanceMap = new HashMap<>();
                    Map<String, String> fatherNameMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("Name");
                        String fatherName = doc.getString("FatherName");
                        String amountStr = doc.getString("Amount");

                        if (name == null || amountStr == null) continue;

                        int amount = 0;
                        try {
                            amount = Integer.parseInt(amountStr);
                        } catch (NumberFormatException ignored) {}

                        advanceMap.put(name, advanceMap.getOrDefault(name, 0) + amount);
                        fatherNameMap.put(name, fatherName != null ? fatherName : "");
                    }

                    totalAdvanceList = new ArrayList<>();
                    for (String name : advanceMap.keySet()) {
                        totalAdvanceList.add(
                                new Worker(
                                        null, // docId not needed
                                        name,
                                        fatherNameMap.get(name),
                                        String.valueOf(advanceMap.get(name)),
                                        null, // reason not needed
                                        null  // requestTime not needed
                                )
                        );
                    }

                    // Update worker count
                    workerCount.setText(String.valueOf(totalAdvanceList.size()));

                    if (totalAdvanceList.isEmpty()) {
                        emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                        recyclerView.setVisibility(RecyclerView.GONE);
                    } else {
                        emptyStateLayout.setVisibility(LinearLayout.GONE);
                        recyclerView.setVisibility(RecyclerView.VISIBLE);
                        adapter = new TotalAdvanceAdapter(totalAdvanceList);
                        recyclerView.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                    recyclerView.setVisibility(RecyclerView.GONE);
                    workerCount.setText("0");
                });
    }
}
