package com.jitendersingh.friendsengineer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class WorkerActivity extends Activity {

    TextView welcomeText;
    String username; // from login activity
    String workerName; // store fetched name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker);

        welcomeText = findViewById(R.id.welcome_text);
        username = getIntent().getStringExtra("username");

        // Fetch the worker name from Firestore, then setup UI after name is loaded
        fetchWorkerNameAndSetupUI(username);
    }

    private void fetchWorkerNameAndSetupUI(String username) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("credentials_worker")
                .whereEqualTo("Username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        workerName = doc.getString("Name");
                        if (workerName != null && !workerName.isEmpty()) {
                            String capitalized = Character.toUpperCase(workerName.charAt(0)) + workerName.substring(1);
                            welcomeText.setText("Welcome, " + capitalized);
                        } else {
                            welcomeText.setText("Welcome, Worker");
                        }
                    } else {
                        welcomeText.setText("Welcome, Worker");
                        workerName = null;
                    }
                    setupButtons();
                })
                .addOnFailureListener(e -> {
                    welcomeText.setText("Welcome, Worker");
                    workerName = null;
                    Toast.makeText(this, "Failed to fetch worker name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setupButtons();
                });
    }

    private void setupButtons() {
        // Setup Request Advance Button
        Button requestAdvanceButton = findViewById(R.id.request_advance_button);
        requestAdvanceButton.setOnClickListener(v -> checkRequestLimitAndShowDialog());

        // Total requested Button
        Button totalAdvanceButton = findViewById(R.id.total_advance_button);
        totalAdvanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(WorkerActivity.this, TotalRequestedActivity.class);
            intent.putExtra("workerName", workerName);
            startActivity(intent);
        });

        // View Schedule Button
        Button viewScheduleBtn = findViewById(R.id.view_schedule_button);
        viewScheduleBtn.setOnClickListener(v -> {
            Intent intent = new Intent(WorkerActivity.this, WorkerScheduleActivity.class);
            intent.putExtra("workerName", workerName);
            startActivity(intent);
        });

        // Setup my detail button
        Button viewMyDetail = findViewById(R.id.my_detail);
        viewMyDetail.setOnClickListener(v -> {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("credentials_worker")
                    .whereEqualTo("Username", username)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            String name = doc.getString("Name");
                            String fatherName = doc.getString("FATHER_S_NAME");

                            Intent intent = new Intent(WorkerActivity.this, WorkerDetailsActivity.class);
                            intent.putExtra("name", name != null ? name : "Unknown");
                            intent.putExtra("father_name", fatherName != null ? fatherName : "Unknown");
                            startActivity(intent);
                        } else {
                            Toast.makeText(WorkerActivity.this, "User info not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(WorkerActivity.this, "Error fetching details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // PF Passbook button
        Button pfPassbookBtn = findViewById(R.id.btn_pf_passbook);
        pfPassbookBtn.setOnClickListener(v -> {
            String url = "https://passbook.epfindia.gov.in/MemberPassBook/login";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        // UAN Portal button
        Button uanPortalBtn = findViewById(R.id.btn_uan_portal);
        uanPortalBtn.setOnClickListener(v -> {
            String url = "https://unifiedportal-mem.epfindia.gov.in/memberinterface/";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });
    }

    // -------- Firestore Request Limit Check and Dialog -----------

    private void checkRequestLimitAndShowDialog() {
        if (workerName == null || workerName.isEmpty()) {
            Toast.makeText(this, "Unable to get user name", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Calculate start and end of current month (IST timezone)
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date monthStart = cal.getTime();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date monthEnd = cal.getTime();

        firestore.collection("Requested_Amount")
                .whereEqualTo("Name", workerName)
                .whereGreaterThanOrEqualTo("RequestTime", monthStart)
                .whereLessThanOrEqualTo("RequestTime", monthEnd)
                .whereIn("Status", java.util.Arrays.asList("Pending", "Accepted","Rejected"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    if (count >= 2) {
                        new AlertDialog.Builder(this)
                                .setTitle("Limit Reached")
                                .setMessage("You have already requested advance 2 times this month. You cannot request more.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        showRequestAdvanceDialog();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to check request limit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showRequestAdvanceDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_request_advance, null);
        final EditText amountInput = dialogView.findViewById(R.id.edit_request_amount);
        final EditText reasonInput = dialogView.findViewById(R.id.edit_reason);

        new AlertDialog.Builder(this)
                .setTitle("Request Advance")
                .setView(dialogView)
                .setPositiveButton("Submit", (dialog, which) -> {
                    String amount = amountInput.getText().toString().trim();
                    String reason = reasonInput.getText().toString().trim();

                    if (amount.isEmpty() || reason.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Request")
                            .setMessage("Do you really want to request an advance of ₹" + amount + "?")
                            .setPositiveButton("Yes", (confDialog, confWhich) -> {
                                insertAdvanceRequest(amount, reason);
                            })
                            .setNegativeButton("No", null)
                            .show();

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void insertAdvanceRequest(String amount, String reason) {
        if (workerName == null) {
            Toast.makeText(this, "Worker name not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Since father name is not available here, you may want to fetch it too from Firestore if needed,
        // For now, we can save "Unknown" or fetch asynchronously before saving.

        String fatherName = "Unknown";

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        com.google.firebase.Timestamp requestTimestamp = com.google.firebase.Timestamp.now();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("Name", workerName);
        requestData.put("FatherName", fatherName);
        requestData.put("Amount", amount);
        requestData.put("Reason", reason);
        requestData.put("RequestTime", requestTimestamp);
        requestData.put("Status", "Pending");

        firestore.collection("Requested_Amount")
                .add(requestData)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Request stored in Firestore", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to store in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

        Toast.makeText(this, "Advance request submitted successfully", Toast.LENGTH_SHORT).show();
    }
}
