package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TotalRequestedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout backButton;
    private TextView requestCount;
    private TextView tvWorkerName;
    private RequestedAmountAdapter adapter;
    private List<RequestedAmountModel> requestList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_requested);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        recyclerView = findViewById(R.id.recycler_total_requested);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        backButton = findViewById(R.id.backButton);
        requestCount = findViewById(R.id.requestCount);
        tvWorkerName = findViewById(R.id.tv_worker_name);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        requestList = new ArrayList<>();
        adapter = new RequestedAmountAdapter(requestList);
        recyclerView.setAdapter(adapter);

        // Receive workerName from Intent
        String workerName = getIntent().getStringExtra("workerName");

        if (workerName != null && !workerName.isEmpty()) {
            tvWorkerName.setText(workerName + "'s Requests");
            loadRequestedAmounts(workerName);
        } else {
            Toast.makeText(this, "No worker name provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadRequestedAmounts(String workerName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Requested_Amount")
                .whereEqualTo("Name", workerName)
                .orderBy("RequestTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    requestList.clear();

                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String amount = doc.getString("Amount");
                        String reason = doc.getString("Reason");
                        String status = doc.getString("Status");
                        com.google.firebase.Timestamp ts = doc.getTimestamp("RequestTime");
                        String date = ts != null ? sdf.format(ts.toDate()) : "";

                        if (amount != null && reason != null && status != null) {
                            requestList.add(new RequestedAmountModel(amount, reason, date, status));
                        }
                    }

                    // Update request count
                    requestCount.setText(String.valueOf(requestList.size()));

                    // Show/hide empty state
                    if (requestList.isEmpty()) {
                        emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                        recyclerView.setVisibility(RecyclerView.GONE);
                    } else {
                        emptyStateLayout.setVisibility(LinearLayout.GONE);
                        recyclerView.setVisibility(RecyclerView.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching requested amounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                    recyclerView.setVisibility(RecyclerView.GONE);
                    requestCount.setText("0");
                });
    }
}
