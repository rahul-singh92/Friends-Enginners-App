package com.jitendersingh.friendsengineer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jitendersingh.friendsengineer.adapters.TotalAdvanceAdapter;
import com.jitendersingh.friendsengineer.models.Worker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotalAdvanceActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout backButton;
    private TextView workerCount;
    private TotalAdvanceAdapter adapter;

    private FirebaseFirestore firestore;

    private EditText searchBar;
    private Spinner filterSpinner;

    private List<Worker> originalList = new ArrayList<>();
    private List<Worker> totalAdvanceList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_total_advance);

        applyEdgeToEdge(R.id.root_layout);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        recyclerView = findViewById(R.id.recycler_total_advance);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        backButton = findViewById(R.id.backButton);
        workerCount = findViewById(R.id.workerCount);
        searchBar = findViewById(R.id.searchBar);
        filterSpinner = findViewById(R.id.filterSpinner);

        backButton.setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();

        // Spinner Filters
        String[] filters = {"All", "Day", "Week", "Month", "Year"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        // Search Listener
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applySearchAndFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Filter Listener
        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                applySearchAndFilter();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        loadTotalAdvance();
    }

    private void loadTotalAdvance() {
        firestore.collection("Requested_Amount")
                .whereEqualTo("Status", "Accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    originalList.clear();

                    if (querySnapshot.isEmpty()) {
                        emptyStateLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        workerCount.setText("0");
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {

                        String name = doc.getString("Name");
                        String fatherName = doc.getString("FatherName");
                        String amountStr = doc.getString("Amount");

                        // Convert Firestore Timestamp to String milliseconds
                        Timestamp ts = doc.getTimestamp("RequestTime");
                        String requestTime = "";

                        if (ts != null) {
                            requestTime = String.valueOf(ts.toDate().getTime()); // milliseconds stored in String
                        }

                        if (name == null || amountStr == null) continue;

                        int amount = 0;
                        try {
                            amount = Integer.parseInt(amountStr);
                        } catch (Exception ignored) {}

                        Worker worker = new Worker(
                                null,
                                name,
                                fatherName != null ? fatherName : "",
                                String.valueOf(amount),
                                null,
                                requestTime
                        );

                        originalList.add(worker);
                    }

                    applySearchAndFilter();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    workerCount.setText("0");
                });
    }

    private void applySearchAndFilter() {

        String searchText = searchBar.getText().toString().toLowerCase().trim();
        String filterType = filterSpinner.getSelectedItem().toString();

        List<Worker> filteredList = new ArrayList<>();

        for (Worker worker : originalList) {

            String name = worker.getName() != null ? worker.getName().toLowerCase() : "";
            String father = worker.getFatherName() != null ? worker.getFatherName().toLowerCase() : "";

            boolean matchesSearch = searchText.isEmpty()
                    || name.contains(searchText)
                    || father.contains(searchText);

            if (!matchesSearch) continue;

            // Filter by date
            if (!filterType.equals("All")) {
                if (!isWithinFilter(worker.getRequestTime(), filterType)) {
                    continue;
                }
            }

            filteredList.add(worker);
        }

        // Group total amount per worker
        Map<String, Integer> advanceMap = new HashMap<>();
        Map<String, String> fatherNameMap = new HashMap<>();

        for (Worker worker : filteredList) {
            String name = worker.getName();
            int amount = 0;

            try {
                amount = Integer.parseInt(worker.getAmount());
            } catch (Exception ignored) {}

            advanceMap.put(name, advanceMap.getOrDefault(name, 0) + amount);
            fatherNameMap.put(name, worker.getFatherName());
        }

        totalAdvanceList.clear();

        for (String name : advanceMap.keySet()) {
            totalAdvanceList.add(new Worker(
                    null,
                    name,
                    fatherNameMap.get(name),
                    String.valueOf(advanceMap.get(name)),
                    null,
                    null
            ));
        }

        // Update UI
        workerCount.setText(String.valueOf(totalAdvanceList.size()));

        if (totalAdvanceList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            adapter = new TotalAdvanceAdapter(totalAdvanceList);
            recyclerView.setAdapter(adapter);
        }
    }

    // requestTime is stored as milliseconds string
    private boolean isWithinFilter(String requestTime, String filterType) {
        if (requestTime == null || requestTime.isEmpty()) return false;

        long millis;
        try {
            millis = Long.parseLong(requestTime);
        } catch (Exception e) {
            return false;
        }

        Date requestDate = new Date(millis);

        Calendar recordCal = Calendar.getInstance();
        recordCal.setTime(requestDate);

        Calendar now = Calendar.getInstance();

        switch (filterType) {
            case "Day":
                return recordCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && recordCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);

            case "Week":
                return recordCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && recordCal.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR);

            case "Month":
                return recordCal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                        && recordCal.get(Calendar.MONTH) == now.get(Calendar.MONTH);

            case "Year":
                return recordCal.get(Calendar.YEAR) == now.get(Calendar.YEAR);
        }

        return true;
    }
}
