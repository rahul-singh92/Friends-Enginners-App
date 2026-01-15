package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkerScheduleActivity extends BaseActivity {

    private ListView listView;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private LinearLayout backButton;
    private TextView tvDepartment;
    private TextView scheduleCount;

    private FirebaseFirestore firestore;

    private String workerName;
    private String department;

    private List<ScheduleItem> scheduleItems = new ArrayList<>();
    private ScheduleAdapter adapter;

    private static final SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    private static final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_schedule);

        applyEdgeToEdge(R.id.root_layout);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        listView = findViewById(R.id.schedule_listview);
        progressBar = findViewById(R.id.progressBar);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        backButton = findViewById(R.id.backButton);
        tvDepartment = findViewById(R.id.tv_department);
        scheduleCount = findViewById(R.id.scheduleCount);

        // Back button handler
        backButton.setOnClickListener(v -> finish());

        firestore = FirebaseFirestore.getInstance();

        workerName = getIntent().getStringExtra("workerName");
        if (workerName == null || workerName.isEmpty()) {
            Toast.makeText(this, "Worker name missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchDepartment();
    }

    private void fetchDepartment() {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        firestore.collection("Worker_Detail")
                .whereEqualTo("Name", workerName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        department = doc.getString("Department");
                        if (department != null && !department.isEmpty()) {
                            tvDepartment.setText(department + " Department");
                            fetchSchedulesForDepartment(department);
                        } else {
                            progressBar.setVisibility(ProgressBar.GONE);
                            tvDepartment.setText("No department");
                            Toast.makeText(this, "Department not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(ProgressBar.GONE);
                        tvDepartment.setText("No department");
                        Toast.makeText(this, "Worker details not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    tvDepartment.setText("Error loading");
                    Toast.makeText(this, "Error fetching department: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchSchedulesForDepartment(String department) {
        firestore.collection("Schedules")
                .whereEqualTo("spinnerOption", department)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    scheduleItems.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String pdfUrl = doc.getString("pdfUrl");
                        String startDateStr = doc.getString("startDate");
                        String endDateStr = doc.getString("endDate");

                        String displayStartDate = formatDate(startDateStr);
                        String displayEndDate = formatDate(endDateStr);

                        scheduleItems.add(new ScheduleItem(doc.getId(), pdfUrl, department, displayStartDate, displayEndDate));
                    }

                    // Update schedule count
                    scheduleCount.setText(String.valueOf(scheduleItems.size()));

                    // Show/hide empty state
                    if (scheduleItems.isEmpty()) {
                        emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                        listView.setVisibility(ListView.GONE);
                    } else {
                        emptyStateLayout.setVisibility(LinearLayout.GONE);
                        listView.setVisibility(ListView.VISIBLE);

                        adapter = new ScheduleAdapter();
                        listView.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    scheduleCount.setText("0");
                    emptyStateLayout.setVisibility(LinearLayout.VISIBLE);
                    listView.setVisibility(ListView.GONE);
                    Toast.makeText(this, "Error fetching schedules: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatDate(String dateStr) {
        if (dateStr == null) return "Unknown Date";
        try {
            Date date = firestoreDateFormat.parse(dateStr);
            if (date != null) {
                return displayDateFormat.format(date);
            } else {
                return dateStr;
            }
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void openPdf(String pdfUrl) {
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(this, "PDF URL is invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(pdfUrl));
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    private static class ScheduleItem {
        String documentId;
        String pdfUrl;
        String department;
        String startDate;
        String endDate;

        ScheduleItem(String documentId, String pdfUrl, String department, String startDate, String endDate) {
            this.documentId = documentId;
            this.pdfUrl = pdfUrl;
            this.department = department;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private class ScheduleAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return scheduleItems.size();
        }

        @Override
        public ScheduleItem getItem(int position) {
            return scheduleItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(WorkerScheduleActivity.this)
                        .inflate(R.layout.item_schedule, parent, false);
            }

            ScheduleItem item = getItem(position);

            TextView txtDepartment = convertView.findViewById(R.id.txt_department);
            TextView txtDateRange = convertView.findViewById(R.id.txt_date_range);

            txtDepartment.setText(item.department);
            txtDateRange.setText(item.startDate + " - " + item.endDate);

            convertView.setOnClickListener(v -> openPdf(item.pdfUrl));

            return convertView;
        }
    }
}
