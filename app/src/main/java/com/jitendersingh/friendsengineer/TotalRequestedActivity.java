package com.jitendersingh.friendsengineer;

import android.os.Bundle;
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
    private RequestedAmountAdapter adapter;
    private List<RequestedAmountModel> requestList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_requested);

        recyclerView = findViewById(R.id.recycler_total_requested);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        requestList = new ArrayList<>();
        adapter = new RequestedAmountAdapter(requestList);
        recyclerView.setAdapter(adapter);

        // Receive workerName from Intent
        String workerName = getIntent().getStringExtra("workerName");

        if (workerName != null && !workerName.isEmpty()) {
            loadRequestedAmounts(workerName);
        } else {
            Toast.makeText(this, "No worker name provided", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRequestedAmounts(String workerName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Requested_Amount")
                .whereEqualTo("Name", workerName)
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

                        requestList.add(new RequestedAmountModel(amount, reason, date, status));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching requested amounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}