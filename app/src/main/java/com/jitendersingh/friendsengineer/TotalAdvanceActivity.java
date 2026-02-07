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
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jitendersingh.friendsengineer.adapters.TotalAdvanceAdapter;
import com.jitendersingh.friendsengineer.models.Worker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
                            requestTime = String.valueOf(ts.toDate().getTime());
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

            // CLICK LISTENER FOR POPUP
            adapter.setOnWorkerClickListener(worker -> {
                showAdvancePopup(worker.getName());
            });

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

    // ================= POPUP LOGIC ==================

    private void showAdvancePopup(String workerName) {

        String filterType = filterSpinner.getSelectedItem().toString();

        List<Worker> workerRecords = new ArrayList<>();

        // collect all records of that worker
        for (Worker w : originalList) {
            if (w.getName() != null && w.getName().equalsIgnoreCase(workerName)) {
                workerRecords.add(w);
            }
        }

        if (workerRecords.isEmpty()) {
            Toast.makeText(this, "No records found", Toast.LENGTH_SHORT).show();
            return;
        }

        // sort by date latest first
        Collections.sort(workerRecords, new Comparator<Worker>() {
            @Override
            public int compare(Worker o1, Worker o2) {
                long t1 = getMillis(o1.getRequestTime());
                long t2 = getMillis(o2.getRequestTime());
                return Long.compare(t2, t1);
            }
        });

        StringBuilder details = new StringBuilder();

        if (filterType.equals("All")) {
            details.append("All Advances:\n\n");

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            for (Worker w : workerRecords) {
                String dateStr = "Unknown";
                try {
                    long millis = Long.parseLong(w.getRequestTime());
                    dateStr = sdf.format(new Date(millis));
                } catch (Exception ignored) {}

                details.append("₹")
                        .append(w.getAmount())
                        .append("  -  ")
                        .append(dateStr)
                        .append("\n");
            }

        } else {

            Map<String, Integer> grouped = new HashMap<>();

            for (Worker w : workerRecords) {
                String key = getGroupKey(w.getRequestTime(), filterType);

                int amt = 0;
                try {
                    amt = Integer.parseInt(w.getAmount());
                } catch (Exception ignored) {}

                grouped.put(key, grouped.getOrDefault(key, 0) + amt);
            }

            details.append(filterType).append(" Summary:\n\n");

            // sort keys (for Month/Year etc.)
            List<String> keys = new ArrayList<>(grouped.keySet());
            Collections.sort(keys);

            for (String key : keys) {
                details.append(key)
                        .append(" : ₹")
                        .append(grouped.get(key))
                        .append("\n");
            }
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_advance_details, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogSubTitle = dialogView.findViewById(R.id.dialogSubTitle);
        TextView dialogDetails = dialogView.findViewById(R.id.dialogDetails);
        TextView btnClose = dialogView.findViewById(R.id.btnClose);

        dialogTitle.setText("Advance Details");
        dialogSubTitle.setText(workerName + " (" + filterType + ")");
        dialogDetails.setText(details.toString());

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.DarkDialogTheme)
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private long getMillis(String requestTime) {
        if (requestTime == null || requestTime.isEmpty()) return 0;
        try {
            return Long.parseLong(requestTime);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getGroupKey(String requestTime, String filterType) {

        long millis = getMillis(requestTime);
        if (millis == 0) return "Unknown";

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);

        if (filterType.equals("Day")) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(millis));
        }

        if (filterType.equals("Week")) {
            int week = cal.get(Calendar.WEEK_OF_YEAR);
            int year = cal.get(Calendar.YEAR);
            return "Week " + week + " (" + year + ")";
        }

        if (filterType.equals("Month")) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(millis));
        }

        if (filterType.equals("Year")) {
            return String.valueOf(cal.get(Calendar.YEAR));
        }

        return "Unknown";
    }
}
