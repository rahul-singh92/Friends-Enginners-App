package com.jitendersingh.friendsengineer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkerScheduleActivity extends AppCompatActivity {

    private ListView listView;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;

    private String workerName;
    private String department;

    private List<ScheduleItem> scheduleItems = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<String> displayList = new ArrayList<>();

    private static final SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
    private static final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_schedule);

        listView = findViewById(R.id.schedule_listview);
        progressBar = findViewById(R.id.progressBar);

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
                            fetchSchedulesForDepartment(department);
                        } else {
                            progressBar.setVisibility(ProgressBar.GONE);
                            Toast.makeText(this, "Department not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressBar.setVisibility(ProgressBar.GONE);
                        Toast.makeText(this, "Worker details not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(ProgressBar.GONE);
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
                    displayList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String pdfUrl = doc.getString("pdfUrl");
                        String startDateStr = doc.getString("startDate");
                        String endDateStr = doc.getString("endDate");

                        String displayStartDate = formatDate(startDateStr);
                        String displayEndDate = formatDate(endDateStr);

                        String displayName = department + " | " + displayStartDate + " - " + displayEndDate;

                        scheduleItems.add(new ScheduleItem(doc.getId(), pdfUrl, displayName));
                        displayList.add(displayName);
                    }

                    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
                    listView.setAdapter(adapter);

                    listView.setOnItemClickListener((parent, view, position, id) -> {
                        ScheduleItem item = scheduleItems.get(position);
                        openPdf(item.pdfUrl);
                    });

                    if (scheduleItems.isEmpty()) {
                        Toast.makeText(this, "No schedules found for department: " + department, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(ProgressBar.GONE);
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
        String displayName;

        ScheduleItem(String documentId, String pdfUrl, String displayName) {
            this.documentId = documentId;
            this.pdfUrl = pdfUrl;
            this.displayName = displayName;
        }
    }

    public static class DataRecordAdapter extends RecyclerView.Adapter<DataRecordAdapter.ViewHolder> {

        private List<DataListActivity.DataItem> dataItems;
        private OnItemClickListener clickListener;
        private OnItemLongClickListener longClickListener;

        public interface OnItemClickListener {
            void onItemClick(DataListActivity.DataItem item);
        }

        public interface OnItemLongClickListener {
            void onItemLongClick(DataListActivity.DataItem item);
        }

        public DataRecordAdapter(List<DataListActivity.DataItem> dataItems,
                                 OnItemClickListener clickListener,
                                 OnItemLongClickListener longClickListener) {
            this.dataItems = dataItems;
            this.clickListener = clickListener;
            this.longClickListener = longClickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_data_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DataListActivity.DataItem item = dataItems.get(position);

            // Set name
            String name = item.getName() != null ? item.getName() : "N/A";
            holder.nameText.setText(name);

            // Set father's name
            String fatherName = item.getFatherName() != null ? item.getFatherName() : "N/A";
            holder.fatherNameText.setText("Father: " + fatherName);

            // Set initial
            if (name != null && !name.isEmpty() && !name.equals("N/A")) {
                holder.initialText.setText(String.valueOf(name.charAt(0)).toUpperCase());
            } else {
                holder.initialText.setText("?");
            }

            // Click listener
            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onItemClick(item);
                }
            });

            // Long click on more options button
            holder.moreOptions.setOnClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(item);
                }
            });
        }

        @Override
        public int getItemCount() {
            return dataItems.size();
        }

        public void updateData(List<DataListActivity.DataItem> newData) {
            this.dataItems = newData;
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView fatherNameText;
            TextView initialText;
            ImageView moreOptions;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.recordName);
                fatherNameText = itemView.findViewById(R.id.recordFatherName);
                initialText = itemView.findViewById(R.id.userInitial);
                moreOptions = itemView.findViewById(R.id.moreOptions);
            }
        }
    }
}
